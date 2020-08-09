package sbtmoped

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

import scala.collection.mutable
import scala.sys.process.Process
import scala.util.Properties
import scala.util.control.NonFatal

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object MopedPlugin extends AutoPlugin {
  override def requires = JvmPlugin
  object autoImport {
    val Moped: Configuration = config("moped")
    val MopedInternal: Configuration = config("moped-internal").hide

    lazy val mopedAlert: TaskKey[String => Unit] =
      taskKey[String => Unit](
        "This function is called with a message when the native image is ready."
      )
    lazy val mopedNativeImageVersion: SettingKey[String] =
      settingKey[String]("The version of GraalVM to use by default.")
    lazy val mopedCoursierBinary: TaskKey[File] =
      taskKey[File](
        "Path to a coursier binary that is used to launch GraalVM native-image."
      )
    lazy val mopedNativeImageBinary: TaskKey[List[String]] =
      taskKey[List[String]](
        "The command arguments to launch the GraalVM native-image binary."
      )
    lazy val mopedNativeImage: TaskKey[File] =
      taskKey[File]("Generate a native image for this project.")
    lazy val mopedNativeImageOptions: TaskKey[List[String]] =
      taskKey[List[String]](
        "Extra command-line arguments that should be forwarded to the native-image optimizer."
      )
  }
  import autoImport._
  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      target.in(Moped) := target.in(Compile).value / "moped",
      target.in(MopedInternal) := target.in(Compile).value / "moped-internal",
      mopedAlert := {
        val s = streams.value

        { message: String => this.alertUser(s, message) }
      },
      mainClass.in(Moped) := mainClass.in(Compile).value,
      mopedNativeImageVersion := "20.1.0",
      name.in(Moped) := name.value,
      mainClass.in(Moped) := mainClass.in(Compile).value,
      mopedNativeImageOptions := List(),
      mopedCoursierBinary := {
        val out = target.in(MopedInternal).value / "coursier"
        Files.createDirectories(out.toPath.getParent)
        val in =
          this.getClass().getResourceAsStream("/sbt-moped/coursier")
        Files.copy(
          in,
          out.toPath,
          StandardCopyOption.REPLACE_EXISTING
        )
        out.setExecutable(true)
        out
      },
      mopedNativeImageBinary := {
        val svmVersion = mopedNativeImageVersion.value
        List(
          mopedCoursierBinary.value.absolutePath,
          "launch",
          "--jvm",
          s"graalvm:$svmVersion",
          s"org.graalvm.nativeimage:svm-driver:$svmVersion",
          "--"
        )
      },
      mopedNativeImage := {
        val main = mainClass.in(Moped).value
        val binaryName = target.in(Moped).value / name.in(Moped).value
        val cp = fullClasspath.in(Compile).value.map(_.data)
        // NOTE(olafur): we pass in a manifest jar instead of the full classpath
        // for two reasons:
        // * large classpaths quickly hit on the "argument list too large"
        //   error, especially on Windows.
        // * we print the full command to the console and the manifest jar makes
        //   it more readable and easier to copy-paste.
        val manifest = target.in(MopedInternal).value / "manifest.jar"
        createManifestJar(manifest, cp)

        // Assemble native-image argument list.
        val command = mutable.ListBuffer.empty[String]
        command ++= mopedNativeImageBinary.value
        command += "-cp"
        command += manifest.absolutePath
        command ++= mopedNativeImageOptions.value
        command += main.getOrElse(
          throw new MessageOnlyException(
            "no mainClass is specified. " +
              "To fix this problem, update build.sbt to include the settings " +
              "`mainClass.in(Compile) := Some(\"com.MainClass\")`"
          )
        )
        command += binaryName.absolutePath

        // Start native-image linker.
        streams.value.log.info(command.mkString(" "))
        val exit = Process(command, cwd = Some(target.in(Moped).value)).!
        if (exit != 0) {
          throw new MessageOnlyException(
            s"native-image command failed with exit code '$exit'"
          )
        }

        mopedAlert.value.apply("Native image ready!")
        streams.value.log.info(binaryName.absolutePath)
        binaryName
      }
    )

  private def isCI = "true".equalsIgnoreCase(System.getenv("CI"))

  private def createManifestJar(manifestJar: File, cp: Seq[File]): Unit = {
    // Add trailing slash to directories so that manifest dir entries work
    val classpathStr =
      cp.map(addTrailingSlashToDirectories).mkString(" ")
    val manifest = new Manifest()
    manifest.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    manifest.getMainAttributes.put(Attributes.Name.CLASS_PATH, classpathStr)
    val out = Files.newOutputStream(manifestJar.toPath)
    // This needs to be declared since jos itself should be set to close as well.
    var jos: JarOutputStream = null
    try {
      jos = new JarOutputStream(out, manifest)
    } finally {
      if (jos == null) {
        out.close()
      } else {
        jos.close()
      }
    }
  }

  private def addTrailingSlashToDirectories(path: File): String = {
    // NOTE(olafur): manifest jars must use URL-encoded paths.
    // https://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html
    val syntax = path.toURI.toURL.getPath
    val separatorAdded = {
      if (syntax.endsWith(".jar")) {
        syntax
      } else {
        syntax + File.separator
      }
    }
    if (Properties.isWin) {
      // Prepend drive letters in windows with slash
      if (separatorAdded.indexOf(":") != 1) separatorAdded
      else File.separator + separatorAdded
    } else {
      separatorAdded
    }
  }

  private def alertUser(streams: std.TaskStreams[_], message: String): Unit = {
    streams.log.info(message)
    if (isCI) return
    try {
      if (Properties.isMac) {
        Process(List("say", message)).!
      }
    } catch {
      case NonFatal(_) =>
    }
  }
}

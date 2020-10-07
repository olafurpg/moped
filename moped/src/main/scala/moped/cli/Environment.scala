package moped.cli

import java.io.BufferedReader
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.{util => ju}

import scala.collection.JavaConverters._

import dev.dirs.ProjectDirectories
import java.time.Clock

final case class Environment(
    dataDirectory: Path,
    cacheDirectory: Path,
    preferencesDirectory: Path,
    workingDirectory: Path = Paths.get(System.getProperty("user.dir")),
    homeDirectory: Path = Paths.get(System.getProperty("user.home")),
    standardOutput: PrintStream = Console.out,
    standardError: PrintStream = Console.err,
    standardInput: BufferedReader = Console.in,
    systemProperties: ju.Properties = System.getProperties(),
    environmentVariables: collection.Map[String, String] =
      System.getenv().asScala,
    clock: Clock = Clock.systemDefaultZone()
) {
  def isColorEnabled: Boolean =
    environmentVariables.get("NO_COLOR").exists(_.equalsIgnoreCase("true"))
  def withProjectDirectories(dirs: ProjectDirectories): Environment =
    copy(
      dataDirectory = Paths.get(dirs.dataDir),
      cacheDirectory = Paths.get(dirs.cacheDir),
      preferencesDirectory = Paths.get(dirs.preferenceDir)
    )
}

object Environment {
  val default: Environment = fromName("moped")
  def fromName(name: String): Environment =
    fromProjectDirectories(ProjectDirectories.fromPath("moped"))
  def fromProjectDirectories(dirs: ProjectDirectories): Environment =
    Environment(
      dataDirectory = Paths.get(dirs.dataDir),
      cacheDirectory = Paths.get(dirs.cacheDir),
      preferencesDirectory = Paths.get(dirs.preferenceDir)
    )
}

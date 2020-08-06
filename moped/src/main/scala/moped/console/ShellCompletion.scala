package moped.console

import java.nio.file.Files
import java.nio.file.Path

import moped.internal.console.Utils
import scala.collection.mutable
import java.nio.file.Paths
import scala.sys.process.ProcessLogger
import scala.util.control.NonFatal

sealed abstract class ShellCompletion(app: Application) {
  def install(): Unit
  def uninstall(): Unit
}
object ShellCompletion {
  def all(app: Application): List[ShellCompletion] =
    List(
      new BashCompletion(app),
      new ZshCompletion(app),
      new FishCompletion(app)
    )
}

final class ZshCompletion(app: Application) extends ShellCompletion(app) {
  def install(): Unit = {
    Utils.overwriteFile(completionFile, completionScript)
    val readableFunctionDirectory = fpath.find(path => Files.isReadable(path))
    readableFunctionDirectory match {
      case Some(dir) =>
        val link = dir.resolve(s"_${app.binaryName}")
        Files.createSymbolicLink(link, completionFile)
        app.info(link.toString())
      case None =>
        app.error(
          "unable to find a readable directory in the zsh function path ($fpath). " +
            "To fix this problem, manually create a symbolic link with the command below:\n" +
            s"\tln -svf '$completionFile' $$(echo $$fpath[1])"
        )
    }
  }

  def uninstall(): Unit = {
    fpath.foreach { path =>
      val script = path.resolve(underscoreName)
      if (Files.isRegularFile(script) && Files.isReadable(script)) {
        Files.delete(script)
      }
    }
  }

  private def underscoreName = s"_${app.binaryName}"
  private def zshrc = app.env.homeDirectory.resolve(".zshrc")
  private def fpath: List[Path] = {
    val paths = mutable.ListBuffer.empty[Path]
    try {
      val process = scala.sys.process
        .Process(List("zsh", "-c", "for f in $fpath; echo $f"))
        .!(ProcessLogger(out => paths += Paths.get(out), err => app.error(err)))
      if (process == 0) paths.toList
      else Nil
    } catch {
      case NonFatal(_) =>
        Nil
    }
  }
  private def completionFile: Path =
    app.configDirectory.resolve("zsh").resolve(underscoreName)
  private def completionScript: String =
    """|#compdef _BINARY_NAME BINARY_NAME
       |
       |# DO NOT EDIT: this script is automatically generated by the command 'BINARY_NAME completions install'.
       |function _BINARY_NAME {
       |    compadd -- $(BINARY_NAME completions run zsh $CURRENT $words[@] 2> /dev/null)
       |}
       |""".stripMargin.replace("BINARY_NAME", app.binaryName)
}

final class BashCompletion(app: Application) extends ShellCompletion(app) {
  def install(): Unit = {
    Utils.overwriteFile(completionFile, completionScript)
    app.info(completionFile.toString())
    if (Files.isRegularFile(bashrc)) {
      Utils.appendLines(
        bashrc,
        List(s"[ -s '$completionFile' ] && source '$completionFile'")
      )
    }
  }
  def uninstall(): Unit = {
    Utils.filterLinesMatching(bashrc, completionFile.toString())
  }
  private def bashrc = app.env.homeDirectory.resolve(".bashrc")
  private def completionFile: Path =
    app.configDirectory.resolve("bash").resolve(s"${app.binaryName}.sh")
  private def completionScript: String =
    """|# DO NOT EDIT: this script is automatically generated by the command 'BINARY_NAME completions install'.
       |_BINARY_NAME() { 
       |  completions=$(BINARY_NAME completions run bash ${#COMP_WORDS[@]} ${COMP_WORDS[@]} 2> /dev/null)
       |  cur="${COMP_WORDS[COMP_CWORD]}"
       |  COMPREPLY=($(compgen -W "$completions" -- $cur))
       |  return 0
       |}
       |complete -F _tests tests
       |""".stripMargin.replace("BINARY_NAME", app.binaryName)
}

final class FishCompletion(app: Application) extends ShellCompletion(app) {
  def install(): Unit = {
    if (Files.isDirectory(completionFile.getParent())) {
      Utils.overwriteFile(completionFile, completionScript)
      app.info(completionFile.toString())
    }
  }
  def uninstall(): Unit = {
    Files.deleteIfExists(completionFile)
  }
  private def completionFile: Path =
    app.env.homeDirectory
      .resolve(".config")
      .resolve("fish")
      .resolve("functions")
      .resolve(s"${app.binaryName}.fish")
  private def completionScript: String =
    """|# DO NOT EDIT: this script is automatically generated by the command 'BINARY_NAME completions install'.
       |function _BINARY_NAME
       |   set -l arguments (commandline -poc)
       |   set -l current (commandline -ct)
       |   BINARY_NAME completions run fish $arguments $current 2> /dev/null
       |end
       |complete -f -c BINARY_NAME -a "(_BINARY_NAME)"
       |""".stripMargin.replace("BINARY_NAME", app.binaryName)
}

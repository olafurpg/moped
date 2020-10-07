package moped.commands

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import moped.annotations.CommandName
import moped.annotations.Description
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.internal.console.Utils
import moped.internal.reporters.Docs._
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper
import org.typelevel.paiges.Doc

class InstallManCommand(app: Application) extends Command {
  override def run(): Int = {
    val manPageCandidates: List[Path] =
      InstallManCommand.manPageDirectories(app.env.environmentVariables)
    manPageCandidates.headOption match {
      case Some(man1) =>
        val out = man1.resolve(app.binaryName + ".1")
        Utils.overwriteFile(out, formatManPage)
      case None =>
        app.error(
          "unable to install man page since there is no readable directory in $PATH that contains a 'bin' directory. " +
            "To fix this problem, run `mkdir /path/to/bin` on a directory where `/path/to` is on $PATH."
        )
        1
    }
    0
  }
  def formatManPage: String = {
    val now =
      LocalDateTime.now(app.env.clock).format(DateTimeFormatter.ISO_DATE)
    val header = TH + Doc.text(app.binaryName) + Doc.text(" 1 ") +
      quoted(now) + Doc.space + quoted(s"${app.binaryName.capitalize} Manual")

    val name = {
      val tagline =
        if (app.tagline.nonEmpty) Doc.text(" \\- ") + Doc.text(app.tagline)
        else Doc.empty
      Doc.text(app.binaryName) + tagline
    }

    val commands: List[Doc] = for {
      c <- app.commands
      if c.nonHidden
    } yield {
      val commandExamples =
        if (c.examples.isEmpty) Doc.empty
        else
          Doc.text("EXAMPLES:") + RS +
            c.examples + Doc.line + RE + Doc.line
      val commandOptions =
        if (c.optionsManpage.isEmpty) Doc.empty
        else
          Doc.text("OPTIONS:") + RS +
            c.optionsManpage + Doc.line + RE + Doc.line
      val subsections =
        Doc.intercalate(Doc.line, List(commandExamples, commandOptions))
      PP + Doc.line +
        Doc.text(c.subcommandName) + RS +
        c.description + blankLine +
        subsections + RE
    }

    val trailing = Doc.intercalate(
      Doc.line,
      List[(String, Doc)](
        "NAME" -> name,
        "USAGE" -> app.usageDoc,
        "DESCRIPTION" -> app.description,
        "EXAMPLES" -> app.examples,
        "COMMANDS" -> Doc.intercalate(Doc.line, commands)
      ).collect {
        case (key, value) if value.nonEmpty =>
          SH + quoted(key) + Doc.line + value
      }
    )
    val result = Doc.intercalate(Doc.line, List(header, trailing))
    result.renderTrim(width = 10000)
  }
}

object InstallManCommand {
  def manPageDirectories(
      env: scala.collection.Map[String, String]
  ): List[Path] =
    env
      .get("PATH")
      .map(_.split(File.pathSeparator))
      .getOrElse(Array.empty)
      .iterator
      .map(Paths.get(_))
      .filter(dir => Files.isDirectory(dir))
      .map(_.resolveSibling("man1"))
      .toList
      .reverse

  implicit lazy val parser: CommandParser[InstallManCommand] =
    new CommandParser[InstallManCommand](
      JsonCodec.encoderDecoderJsonCodec(
        ClassShaper(
          new ClassShape(
            "InstallManCommand",
            "moped.commands.InstallManCommand",
            List(),
            List(
              CommandName("install"),
              Description("Install man page documentation")
            )
          )
        ),
        JsonEncoder.stringJsonEncoder
          .contramap[InstallManCommand](_ => ""),
        JsonDecoder.applicationJsonDecoder.map(app =>
          new InstallManCommand(app)
        )
      ),
      new InstallManCommand(Application.default)
    )
}

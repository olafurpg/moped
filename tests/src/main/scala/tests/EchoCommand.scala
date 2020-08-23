package tests

import moped.annotations.PositionalArguments
import moped.annotations._
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.commands.CompletionsCommand
import moped.commands.HelpCommand
import moped.commands.VersionCommand
import moped.internal.console.Utils
import moped.json.JsonArray
import moped.json.JsonString

@Description("Write arguments to the standard output")
@ExampleUsage(
  """|$ echo hello world!
     |hello world!
     |$ echo --uppercase hello world!
     |HELLO WORLD!
     |""".stripMargin
)
case class EchoCommand(
    @Description("If true, the output will be all UPPERCASE")
    uppercase: Boolean = false,
    @Description("If false, the output will be changed to '--no-unchanged'")
    unchanged: Boolean = true,
    @Description("If false, the output will be all lowercase")
    noLowercase: Boolean = true,
    @TrailingArguments()
    trailing: List[String] = Nil,
    @PositionalArguments()
    args: List[String] = Nil
) extends Command {
  def run(app: Application): Int = {
    val out = app.env.standardOutput
    val toPrint =
      if (!unchanged) List("--no-unchanged")
      else if (uppercase) args.map(_.toUpperCase())
      else if (!noLowercase) args.map(_.toLowerCase())
      else args
    out.println(toPrint.mkString(" "))
    if (trailing.nonEmpty) {
      out.println(trailing.mkString(" "))
    }
    0
  }
}

object EchoCommand {
  implicit val parser: CommandParser[EchoCommand] =
    CommandParser.derive(EchoCommand())
  lazy val app: Application = Application.fromName(
    "tests",
    "1.0.0",
    commands = List(
      CommandParser[HelpCommand],
      CommandParser[VersionCommand],
      CommandParser[TerminalsCommand],
      CommandParser[EchoCommand],
      CommandParser[ConfigCommand],
      CommandParser[CompletionsCommand]
    )
  )
  def main(args: Array[String]): Unit = {
    Utils.appendLines(
      app.env.homeDirectory.resolve(".dump"),
      List(JsonArray(args.map(JsonString(_)).toList).toDoc.render(80))
    )
    app.runAndExitIfNonZero(args.toList)
  }
}

package tests

import moped.annotations.PositionalArguments
import moped.annotations._
import moped.commands.CompletionsCommand
import moped.commands.HelpCommand
import moped.commands.VersionCommand
import moped.console.Application
import moped.console.Command
import moped.console.CommandParser
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
    val toPrint =
      if (!unchanged) List("--no-unchanged")
      else if (uppercase) args.map(_.toUpperCase())
      else if (!noLowercase) args.map(_.toLowerCase())
      else args
    app.env.standardOutput.println(toPrint.mkString(" "))
    if (trailing.nonEmpty) {
      app.env.standardOutput.println(trailing.mkString(" "))
    }
    0
  }
}

object EchoCommand {
  implicit val parser: CommandParser[EchoCommand] =
    CommandParser.derive(EchoCommand())
  def main(args: Array[String]): Unit = {
    val app = Application(
      "tests",
      "1.0.0",
      commands = List(
        CommandParser[HelpCommand],
        CommandParser[VersionCommand],
        CommandParser[TerminalsCommand],
        CommandParser[EchoCommand],
        CommandParser[CompletionsCommand]
      )
    )
    Utils.appendLines(
      app.env.homeDirectory.resolve(".dump"),
      List(JsonArray(args.map(JsonString(_)).toList).toDoc.render(80))
    )
    app.runAndExitIfNonZero(args.toList)
  }
}

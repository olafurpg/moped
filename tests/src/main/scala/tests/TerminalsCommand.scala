package tests

import moped.annotations.Description
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.reporters.Terminals

@Description("Print the console screen width")
final case class TerminalsCommand() extends Command {
  def run(app: Application): Int = {
    app.out.println(Terminals.screenWidth())
    0
  }
}

object TerminalsCommand {
  implicit val parser: CommandParser[TerminalsCommand] =
    CommandParser.derive(TerminalsCommand())
}

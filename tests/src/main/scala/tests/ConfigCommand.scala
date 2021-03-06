package tests

import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser

case class ConfigCommand(
    foobar: Boolean = false,
    app: Application = Application.default
) extends Command {
  def run(): Int = {
    if (foobar) app.out.println("foobar")
    else app.out.println("no foobar")
    0
  }
}

object ConfigCommand {
  implicit lazy val parser: CommandParser[ConfigCommand] =
    CommandParser.derive(ConfigCommand())
}

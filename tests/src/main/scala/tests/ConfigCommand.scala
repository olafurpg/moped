package tests

import moped.console.Application
import moped.console.Command
import moped.console.CommandParser

case class ConfigCommand(
    foobar: Boolean = false
) extends Command {
  def run(app: Application): Int = {
    if (foobar) app.out.println("foobar")
    else app.out.println("no foobar")
    0
  }
}

object ConfigCommand {
  implicit lazy val parser: CommandParser[ConfigCommand] = CommandParser.derive(ConfigCommand())
}

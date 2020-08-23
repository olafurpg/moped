package tests

import java.nio.file.Path

import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser

object WorkingDirectoryCommand {
  implicit val parser: CommandParser[WorkingDirectoryCommand] =
    CommandParser.derive(WorkingDirectoryCommand())
}

case class WorkingDirectoryCommand(
    home: Option[Path] = None
) extends Command {

  override def run(app: Application): Int = ???

}

package moped.commands

import moped.annotations.CommandName
import moped.annotations.Description
import moped.cli.Application
import moped.cli.CommandParser
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper

object ManCommand {

  implicit lazy val parser: CommandParser[ManCommand] =
    new CommandParser[ManCommand](
      JsonCodec.encoderDecoderJsonCodec(
        ClassShaper(
          new ClassShape(
            "ManCommand",
            "moped.commands.ManCommand",
            List(),
            List(
              CommandName("man"),
              Description("Manage man page installation and uninstallation"),
              moped.annotations.Subcommand(InstallManCommand.parser),
              moped.annotations.Subcommand(UninstallManCommand.parser),
              // TODO(olafur): remove help command and fix failing tests. The
              // error message is cryptic when the help command is missing.
              moped.annotations.Subcommand(HelpCommand.parser)
            )
          )
        ),
        JsonEncoder.stringJsonEncoder.contramap[ManCommand](_ => ""),
        JsonDecoder.applicationJsonDecoder.map(app => new ManCommand(app))
      ),
      new ManCommand(Application.default)
    )
}

class ManCommand(app: Application) extends NestedCommand {
  override def run(): Int = {
    app.process("man", "")
    ???
  }
}

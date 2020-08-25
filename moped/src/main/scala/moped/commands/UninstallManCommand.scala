package moped.commands

import moped.annotations.CommandName
import moped.annotations.Description
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper

class UninstallManCommand(app: Application) extends Command {
  override def run(): Int = {
    ???
    0
  }
}

object UninstallManCommand {

  implicit lazy val parser: CommandParser[UninstallManCommand] =
    new CommandParser[UninstallManCommand](
      JsonCodec.encoderDecoderJsonCodec(
        ClassShaper(
          new ClassShape(
            "UninstallManCommand",
            "moped.commands.UninstallManCommand",
            List(),
            List(
              CommandName("uninstall"),
              Description("Uninstall man page documentation")
            )
          )
        ),
        JsonEncoder.stringJsonEncoder
          .contramap[UninstallManCommand](_ => ""),
        JsonDecoder.applicationJsonDecoder.map(app =>
          new UninstallManCommand(app)
        )
      ),
      new UninstallManCommand(Application.default)
    )
}

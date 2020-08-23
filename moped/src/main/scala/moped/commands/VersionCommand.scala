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

object VersionCommand {
  val default = new VersionCommand(Application.default)

  implicit lazy val parser: CommandParser[VersionCommand] =
    new CommandParser[VersionCommand](
      JsonCodec.encoderDecoderJsonCodec(
        ClassShaper(
          new ClassShape(
            "VersionCommand",
            "moped.commands.VersionCommand",
            List(),
            List(
              CommandName("version"),
              Description("Print the version of this program")
            )
          )
        ),
        JsonEncoder.stringJsonEncoder.contramap[VersionCommand](_ => ""),
        JsonDecoder.constant(default)
      ),
      default
    )
}

class VersionCommand(app: Application) extends Command {
  override def run(): Int = {
    app.out.println(app.version)
    0
  }
}

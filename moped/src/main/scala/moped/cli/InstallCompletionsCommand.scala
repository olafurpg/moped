package moped.cli

import moped.annotations.CommandName
import moped.annotations.Description
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper

class InstallCompletionsCommand extends Command {
  override def run(app: Application): Int = {
    ShellCompletion.all(app).foreach { shell =>
      shell.uninstall()
      shell.install()
    }
    0
  }
}

object InstallCompletionsCommand {
  val default = new InstallCompletionsCommand()

  implicit lazy val parser: CommandParser[InstallCompletionsCommand] =
    new CodecCommandParser[InstallCompletionsCommand](
      JsonCodec.encoderDecoderJsonCodec(
        ClassShaper(
          new ClassShape(
            "InstallCompletionsCommand",
            "moped.commands.InstallCompletionsCommand",
            List(),
            List(
              CommandName("install"),
              Description("Install tab completions scripts")
            )
          )
        ),
        JsonEncoder.stringJsonEncoder
          .contramap[InstallCompletionsCommand](_ => ""),
        JsonDecoder.constant(default)
      ),
      default
    )
}

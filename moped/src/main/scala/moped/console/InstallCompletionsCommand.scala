package moped.console

import moped.annotations.CommandName
import moped.annotations.Description
import moped.annotations.Hidden
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper

@CommandName("install-completions")
class InstallCompletionsCommand extends Command {
  override def run(app: Application): Int = {
    ShellCompletion.all(app).foreach { shell =>
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
              Hidden(),
              Description("Install tab completions for bash, zsh and fish")
            )
          )
        ),
        JsonEncoder.stringJsonEncoder.contramap[InstallCompletionsCommand](_ =>
          ""
        ),
        JsonDecoder.constant(default)
      ),
      default
    )
}
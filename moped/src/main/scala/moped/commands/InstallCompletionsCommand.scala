package moped.commands

import moped.annotations.CommandName
import moped.annotations.Description
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.cli.ShellCompletion
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper

class InstallCompletionsCommand(app: Application) extends Command {
  override def run(): Int = {
    ShellCompletion.all(app).foreach { shell =>
      shell.uninstall()
      shell.install()
    }
    0
  }
}

object InstallCompletionsCommand {

  implicit lazy val parser: CommandParser[InstallCompletionsCommand] =
    new CommandParser[InstallCompletionsCommand](
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
        JsonDecoder.applicationJsonDecoder.map(app =>
          new InstallCompletionsCommand(app)
        )
      ),
      new InstallCompletionsCommand(Application.default)
    )
}

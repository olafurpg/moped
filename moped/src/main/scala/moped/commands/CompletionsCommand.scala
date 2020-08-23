package moped.commands

import moped.annotations.CommandName
import moped.annotations.Description
import moped.cli.CommandParser
import moped.cli.InstallCompletionsCommand
import moped.cli.UninstallCompletionsCommand
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper

object CompletionsCommand {
  val default = new CompletionsCommand()

  implicit lazy val parser: CommandParser[CompletionsCommand] =
    new CommandParser[CompletionsCommand](
      JsonCodec.encoderDecoderJsonCodec(
        ClassShaper(
          new ClassShape(
            "CompleteCommand",
            "moped.commands.CompleteCommand",
            List(),
            List(
              CommandName("completions"),
              Description("Manage tab completions for bash, zsh and fish"),
              moped.annotations.Subcommand(HelpCommand.parser),
              moped.annotations.Subcommand(InstallCompletionsCommand.parser),
              moped.annotations.Subcommand(
                UninstallCompletionsCommand.parser
              ),
              moped.annotations.Subcommand(RunCompletionsCommand.parser)
            )
          )
        ),
        JsonEncoder.stringJsonEncoder.contramap[CompletionsCommand](_ => ""),
        JsonDecoder.constant(default)
      ),
      default
    )
}

class CompletionsCommand extends NestedCommand

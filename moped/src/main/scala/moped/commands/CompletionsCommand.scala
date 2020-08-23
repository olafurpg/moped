package moped.commands
import moped.annotations.CatchInvalidFlags
import moped.annotations.CommandName
import moped.annotations.Description
import moped.annotations.PositionalArguments
import moped.cli.CommandParser
import moped.cli.InstallCompletionsCommand
import moped.cli.UninstallCompletionsCommand
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper
import moped.macros.ParameterShape

object CompletionsCommand {
  val default = new CompletionsCommand()

  implicit lazy val parser: CommandParser[CompletionsCommand] =
    new CommandParser[CompletionsCommand](
      JsonCodec.encoderDecoderJsonCodec(
        ClassShaper(
          new ClassShape(
            "CompleteCommand",
            "moped.commands.CompleteCommand",
            List(
              List(
                new ParameterShape(
                  "arguments",
                  "List[String]",
                  List(PositionalArguments(), CatchInvalidFlags()),
                  None
                )
              )
            ),
            List(
              CommandName("completions"),
              Description("Manage tab completions for bash, zsh and fish"),
              moped.annotations.NestedCommand(HelpCommand.parser),
              moped.annotations.NestedCommand(InstallCompletionsCommand.parser),
              moped.annotations.NestedCommand(
                UninstallCompletionsCommand.parser
              ),
              moped.annotations.NestedCommand(RunCompletionsCommand.parser)
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

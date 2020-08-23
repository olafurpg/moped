package moped.commands

import scala.collection.immutable.Nil

import moped.annotations.CatchInvalidFlags
import moped.annotations.CommandName
import moped.annotations.Description
import moped.annotations.PositionalArguments
import moped.cli.Application
import moped.cli.BashCompletion
import moped.cli.CodecCommandParser
import moped.cli.Command
import moped.cli.CommandParser
import moped.cli.FishCompletion
import moped.cli.ShellCompletion
import moped.cli.TabCompletionContext
import moped.cli.TabCompletionItem
import moped.cli.ZshCompletion
import moped.internal.console.Cases
import moped.internal.console.CommandLineParser
import moped.internal.console.Utils
import moped.internal.json.NumberExtractor
import moped.json.JsonArray
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.json.JsonString
import moped.macros.ClassShape
import moped.macros.ClassShaper
import moped.macros.ParameterShape

object RunCompletionsCommand {

  implicit lazy val parser: CommandParser[RunCompletionsCommand] = {
    val default = new RunCompletionsCommand(Application.default)
    new CodecCommandParser[RunCompletionsCommand](
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
              CommandName("run"),
              Description("Print tab completions given command arguments")
            )
          )
        ),
        JsonEncoder.stringJsonEncoder.contramap[RunCompletionsCommand](_ => ""),
        JsonDecoder.applicationJsonDecoder.map(app =>
          new RunCompletionsCommand(app)
        )
      ),
      default
    )
  }
}

class RunCompletionsCommand(app: Application) extends Command {
  def run(): Int = {
    val (format, argumentLength, arguments) = app.relativeArguments match {
      case "zsh" :: NumberExtractor(argumentLength) :: tail =>
        (new ZshCompletion(app), argumentLength.toInt, tail)
      case "bash" :: NumberExtractor(argumentLength) :: tail =>
        (new BashCompletion(app), argumentLength.toInt, tail)
      case "fish" :: tail =>
        // Fish completions pass in an empty string "" as the last argument so we
        // don't need the second argument to know `tail.length`
        (new FishCompletion(app), tail.length, tail)
      case els =>
        app.error(
          s"invalid arguments $els, to fix this problem pass in '${app.consumedArguments.mkString(" ")} $$SHELL $$ARGUMENTS_LENGTH $$ARGUMENTS'. "
        )
        return 1
    }
    val isMissingTrailingEmptyString =
      argumentLength == arguments.length + 1
    val argumentsWithTrailingEmptyString =
      if (isMissingTrailingEmptyString) arguments :+ ""
      else arguments
    val argumentsWithoutBinaryName = argumentsWithTrailingEmptyString.drop(1)
    val completionItems = completions(argumentsWithoutBinaryName, app, format)
    renderCompletions(completionItems, app)
    0
  }

  private def completions(
      arguments: List[String],
      app: Application,
      format: ShellCompletion
  ): List[TabCompletionItem] = {
    def loop(
        rest: List[String],
        relativeCommands: List[CommandParser[_]]
    ): List[TabCompletionItem] = {
      rest match {
        case _ :: Nil =>
          relativeCommands
            .filterNot(_.isHidden)
            .map(p => TabCompletionItem(p.subcommandName))
        case subcommandName :: head :: tail =>
          relativeCommands.find(_.matchesName(subcommandName)) match {
            case Some(subcommand) =>
              if (subcommand.nestedCommands.nonEmpty) {
                loop(head :: tail, subcommand.nestedCommands)
              } else {
                subcommandCompletions(
                  format,
                  subcommand,
                  head,
                  tail,
                  app
                )
              }
            case None =>
              Nil
          }
        case _ =>
          Nil
      }
    }
    loop(arguments, app.commands)
  }

  private def subcommandCompletions(
      shell: ShellCompletion,
      subcommand: CommandParser[_],
      head: String,
      tail: List[String],
      app: Application
  ): List[TabCompletionItem] = {
    val last = tail.lastOption.getOrElse(head)
    val inlined =
      CommandLineParser.allSettings(subcommand).filter {
        case (_, param) =>
          !param.isHidden ||
            !param.isPositionalArgument
      }
    val secondLast = (head :: tail).takeRight(2) match {
      case flag :: _ :: Nil => Some(flag)
      case _ => None
    }
    val setting = secondLast.flatMap(flag =>
      inlined.get(Cases.kebabToCamel(flag.stripPrefix("--")))
    )
    val context = TabCompletionContext(
      shell,
      head :: tail,
      last,
      secondLast,
      setting,
      inlined,
      app
    )
    subcommandCompletions(subcommand, context)
  }

  private def subcommandCompletions(
      command: CommandParser[_],
      context: TabCompletionContext
  ): List[TabCompletionItem] = {
    if (context.last.startsWith("-")) {
      flagCompletions(context)
    } else {
      context.setting match {
        case Some(setting) =>
          if (setting.isTabCompleteOneOf) {
            setting.tabCompleteOneOf.toList.flatten.map(oneof =>
              TabCompletionItem(oneof)
            )
          } else if (setting.isTabComplete) {
            setting.tabCompleter.toList.flatMap(_.complete(context))
          } else {
            command.complete(context)
          }
        case None =>
          command.complete(context)
      }
    }
  }

  private def flagCompletions(
      context: TabCompletionContext
  ): List[TabCompletionItem] = {
    context.allSettings
      .filterNot {
        case (_, setting) =>
          setting.isPositionalArgument ||
            setting.isHidden
      }
      .keys
      .toList
      .sorted
      .map(camel => TabCompletionItem("--" + Cases.camelToKebab(camel)))
  }

  private def renderCompletions(
      items: List[TabCompletionItem],
      app: Application
  ): Unit = {
    Utils.appendLines(
      app.env.homeDirectory.resolve(".dump"),
      List(
        JsonArray(items.map(i => JsonString(i.name)).toList).toDoc.render(80)
      )
    )
    items.foreach { item =>
      app.out.println(item.name)
    }
  }

}

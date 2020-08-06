package moped.commands

import scala.collection.immutable.Nil

import moped.annotations.CatchInvalidFlags
import moped.annotations.CommandName
import moped.annotations.Description
import moped.annotations.PositionalArguments
import moped.annotations.TabCompleter
import moped.console.Application
import moped.console.CodecCommandParser
import moped.console.Command
import moped.console.CommandParser
import moped.console.Completer
import moped.console.NotRecognizedCommand
import moped.console.TabCompletionItem
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper
import moped.macros.ParameterShape
import moped.reporters.Terminals
import org.typelevel.paiges.Doc
import moped.annotations.ExtraName

object HelpCommand {
  val completer: Completer[List[String]] = { context =>
    if (context.arguments.length == 1) {
      context.app.commands.iterator
        .filterNot(_.isHidden)
        .map(c => TabCompletionItem(c.subcommandName))
        .toList
    } else {
      Nil
    }
  }

  def insertHelpFlag(shape: ClassShape): ClassShape = {
    shape.copy(
      parameters = List(
        new ParameterShape(
          "help",
          "Boolean",
          List(
            ExtraName("-h"),
            ExtraName("-help"),
            ExtraName("--help"),
            Description("Print this help message")
          ),
          None
        )
      ) :: shape.parameters
    )
  }
  def swapTrailingHelpFlag(arguments: List[String]): List[String] = {
    def loop(l: List[String]): List[String] =
      l match {
        case command :: ("--help" | "-h" | "-help") :: Nil =>
          "help" :: command :: Nil
        case Nil => Nil
        case head :: tail =>
          head :: loop(tail)
      }
    loop(arguments)
  }

  def parser(help: HelpCommand): CommandParser[HelpCommand] =
    new CodecCommandParser[HelpCommand](
      JsonCodec.encoderDecoderJsonCodec(
        ClassShaper(
          new ClassShape(
            "HelpCommand",
            "moped.commands.HelpCommand",
            List(
              List(
                new ParameterShape(
                  "arguments",
                  "List[String]",
                  List(
                    TabCompleter(completer),
                    PositionalArguments(),
                    CatchInvalidFlags()
                  ),
                  None
                )
              )
            ),
            List(
              CommandName("help", "-h", "--help", "-help"),
              Description("Print this help message")
            )
          )
        ),
        JsonEncoder.stringJsonEncoder.contramap[HelpCommand](_ => ""),
        JsonDecoder.constant(help)
      ),
      help
    )
  implicit lazy val parser: CommandParser[HelpCommand] =
    parser(new HelpCommand())
}

class HelpCommand(
    screenWidth: Int = Terminals.screenWidth(),
    appUsage: Application => Doc = app =>
      Doc.text(s"${app.binaryName} COMMAND [OPTIONS]"),
    appDescription: Application => Doc = _ => Doc.empty,
    appExamples: Application => Doc = _ => Doc.empty
) extends Command {
  override def run(app: Application): Int = {
    app.relativeArguments match {
      case Nil =>
        val usage = appUsage(app)
        if (usage.nonEmpty) {
          app.out.println(s"USAGE:")
          app.out.println(
            usage.indent(2).renderTrim(screenWidth)
          )
        }
        val description = appDescription(app)
        if (description.nonEmpty) {
          if (usage.nonEmpty) app.out.println()
          app.out.println(s"DESCRIPTION:")
          app.out.println(description.indent(2).renderTrim(screenWidth))
        }
        if (app.relativeCommands.nonEmpty) {
          val rows = app.relativeCommands.map { command =>
            command.subcommandName -> command.description
          }
          val message = Doc.tabulate(' ', "  ", rows).indent(2)
          if (usage.nonEmpty) app.out.println()
          app.out.println(s"COMMANDS:")
          app.out.println(message.renderTrim(screenWidth))
          app.out.println(
            (Doc.text(s"See '${app.binaryName} help COMMAND' ") +
              Doc.paragraph(s"for more information on a specific command."))
              .renderTrim(screenWidth)
          )
        }
        val examples = appExamples(app)
        if (examples.nonEmpty) {
          if (app.relativeCommands.nonEmpty) app.out.println()
          app.out.println(s"EXAMPLES:")
          app.out.println(examples.indent(2).renderTrim(screenWidth))
        }
        0
      case subcommand :: Nil =>
        app.relativeCommands.find(_.matchesName(subcommand)) match {
          case Some(command) =>
            command.helpMessage(app.out, screenWidth)
            0
          case None =>
            NotRecognizedCommand.notRecognized(subcommand, app)
            1
        }
      case obtained =>
        app.error(
          s"expected 1 argument but obtained ${obtained.length} arguments " +
            obtained.mkString("'", " ", "'")
        )
        1
    }
  }

}

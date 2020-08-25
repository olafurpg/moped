package moped.commands

import scala.collection.immutable.Nil
import scala.collection.mutable

import moped.annotations.CatchInvalidFlags
import moped.annotations.CommandName
import moped.annotations.Description
import moped.annotations.ExtraName
import moped.annotations.PositionalArguments
import moped.annotations.TabCompleter
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.cli.Completer
import moped.cli.TabCompletionItem
import moped.json.JsonCodec
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.macros.ClassShape
import moped.macros.ClassShaper
import moped.macros.ParameterShape
import org.typelevel.paiges.Doc

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
  def moveFlagsBehindSubcommand(arguments: List[String]): List[String] = {
    val flagsToMoveBehindSubcommand = mutable.ListBuffer.empty[String]
    def loop(args: List[String]): List[String] =
      args match {
        case Nil => flagsToMoveBehindSubcommand.toList
        case head :: tail =>
          if (head.startsWith("-")) {
            flagsToMoveBehindSubcommand += head
            loop(tail)
          } else {
            head :: flagsToMoveBehindSubcommand.prependToList(tail)
          }
      }
    loop(arguments)
  }
  def swapTrailingHelpFlag(arguments: List[String]): List[String] = {
    def loop(l: List[String]): List[String] =
      l match {
        case command :: ("--help" | "-h" | "-help") :: Nil =>
          "help" :: command :: Nil
        case command :: ("--version" | "-v" | "-version") :: Nil =>
          "version" :: command :: Nil
        case Nil => Nil
        case head :: tail =>
          head :: loop(tail)
      }
    loop(arguments)
  }

  def parser(help: Application => HelpCommand): CommandParser[HelpCommand] =
    new CommandParser[HelpCommand](
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
        JsonDecoder.applicationJsonDecoder.map(app => help(app))
      ),
      help(Application.default)
    )
  implicit lazy val parser: CommandParser[HelpCommand] =
    parser(app => new HelpCommand(app))
}

class HelpCommand(app: Application) extends Command {
  override def run(): Int = {
    app.relativeArguments match {
      case Nil =>
        val sections = app.documentation.collect {
          case (key, value) if value.nonEmpty =>
            Doc.text(key) + Doc.line + value.indent(2)
        }
        val help = Doc.intercalate(Doc.line + Doc.line, sections)
        app.out.println(help.renderTrim(app.terminal.screenWidth()))
        0
      case subcommand :: Nil =>
        app.relativeCommands.find(_.matchesName(subcommand)) match {
          case Some(command) =>
            command.helpMessage(app.out, app.terminal.screenWidth())
            0
          case None =>
            new NotRecognizedCommand(app).notRecognized(subcommand)
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

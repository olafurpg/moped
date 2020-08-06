package moped.console

import java.io.PrintStream

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

import moped.annotations.CommandName
import moped.annotations.Hidden
import moped.annotations.TabCompleter
import moped.internal.console.Cases
import moped.internal.console.CommandLineParser
import moped.json._
import moped.macros._
import org.typelevel.paiges.Doc

trait CommandParser[A <: BaseCommand] extends JsonCodec[A] {
  type Value = A
  def asClassShaper: ClassShaper[Value] = this
  def asDecoder: JsonDecoder[Value] = this
  def description: Doc = this.commandLineDescription.getOrElse(Doc.empty)
  def usage: Doc = this.commandLineUsage.getOrElse(Doc.empty)
  def options: Doc = Doc.empty
  def examples: Doc = Doc.intercalate(Doc.line, this.commandLineExamples)
  def isHidden: Boolean = annotations.contains(Hidden())
  def matchesName(name: String): Boolean =
    subcommandNames.exists(_.equalsIgnoreCase(name))
  def helpMessageSections: List[(String, Doc)] =
    List(
      "USAGE:" -> usage,
      "DESCRIPTION:" -> description,
      "OPTIONS:" -> options,
      "EXAMPLES:" -> examples
    )
  def helpMessage: Doc = {
    val docs = helpMessageSections.collect {
      case (key, doc) if doc.nonEmpty =>
        Doc.text(key) + Doc.line + doc.indent(2)
    }
    val blank = Doc.line + Doc.line
    Doc.intercalate(blank, docs)
  }
  final def helpMessage(out: PrintStream, width: Int): Unit = {
    out.println(helpMessage.renderTrim(width))
  }
  def nestedCommands: List[CommandParser[_]] = Nil
  def subcommandName: String =
    subcommandNames.headOption.getOrElse(fallbackSubcommandName)
  def subcommandNames: List[String] = {
    val fromAnnotations = annotations.flatMap {
      case CommandName(names @ _*) => names.toList
      case _ => Nil
    }
    if (fromAnnotations.isEmpty) List(fallbackSubcommandName)
    else fromAnnotations
  }
  private def fallbackSubcommandName =
    Cases.camelToKebab(shape.name).stripSuffix("-command").toLowerCase()
  def decodeCommand(context: DecodingContext): DecodingResult[BaseCommand] =
    this.decode(context)
  def parseCommand(arguments: List[String]): DecodingResult[BaseCommand] =
    CommandLineParser
      .parseArgs[A](arguments)(this)
      .flatMap(elem => decodeCommand(DecodingContext(elem)))
  def withTabCompletion(
      fn: TabCompletionContext => List[TabCompletionItem]
  ): CommandParser[A] = ???
  def complete(context: TabCompletionContext): List[TabCompletionItem] = {
    val allAnnotations: Iterator[StaticAnnotation] = Iterator(
      annotations.iterator,
      parametersFlat.iterator
        .filter(_.isPositionalArgument)
        .flatMap(_.annotations.iterator)
    ).flatten
    val completer = allAnnotations
      .collectFirst { case TabCompleter(fn) => fn }
      .getOrElse(Completer.empty)
    completer.complete(context)
  }
}

object CommandParser {
  def derive[A](default: A): CommandParser[A] =
    macro moped.internal.macros.Macros.deriveCommandParserImpl[A]
  def apply[A <: BaseCommand](implicit ev: CommandParser[A]): CommandParser[A] =
    ev
  def fromCodec[A <: BaseCommand](
      codec: JsonCodec[A],
      default: A
  ): CommandParser[A] =
    new CodecCommandParser(codec, default)

}

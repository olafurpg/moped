package moped.macros

import scala.annotation.StaticAnnotation

import moped.annotations._
import moped.cli.Completer
import moped.internal.console.CommandLineParser
import org.typelevel.paiges.Doc

/**
 * Metadata about one parameter of a class.
 *
 * @param name the parameter name of this parameter.
 * @param tpe the pretty-printed type of this parameter
 * @param annotations static annotations attached to this parameter.
 */
final case class ParameterShape(
    val name: String,
    val tpe: String,
    val annotations: List[StaticAnnotation],
    val underlying: Option[ClassShaper[_]]
) {

  def alternativeNames: List[String] =
    extraNames ::: deprecatedNames.map(_.name)
  def allNames: List[String] = name :: alternativeNames
  def matchesLowercase(name: String): Boolean =
    allNames.exists(_.equalsIgnoreCase(name))

  def description: Option[Doc] =
    annotations.collectFirst {
      case Description(value) => Doc.paragraph(value)
      case DescriptionDoc(value) => value
    }
  def extraNames: List[String] =
    annotations.collect {
      case PositionalArguments() => CommandLineParser.PositionalArgument
      case TrailingArguments() => CommandLineParser.TrailingArgument
      case ExtraName(value) => value
    }
  def deprecatedNames: List[DeprecatedName] =
    annotations.collect {
      case d: DeprecatedName => d
    }
  def exampleValues: List[String] =
    annotations.collect {
      case ExampleValue(value) => value
    }
  def sinceVersion: Option[String] =
    annotations.collectFirst {
      case SinceVersion(value) => value
    }
  def deprecated: Option[Deprecated] =
    annotations.collectFirst {
      case value: Deprecated => value
    }
  def tabCompleteOneOf: Option[List[String]] =
    annotations.collectFirst {
      case oneof: TabCompleteAsOneOf => oneof.options.toList
    }
  def tabCompleter: Option[Completer[_]] =
    annotations.collectFirst {
      case TabCompleter(completer) => completer
    }

  def isRepeated: Boolean =
    annotations.exists(_.isInstanceOf[Repeated])
  def isDynamic: Boolean =
    annotations.exists(_.isInstanceOf[Dynamic])
  def isHidden: Boolean =
    annotations.exists(_.isInstanceOf[Hidden])
  def isBoolean: Boolean =
    annotations.exists(_.isInstanceOf[Flag])
  def isNumber: Boolean =
    annotations.exists(_.isInstanceOf[ParseAsNumber])
  def isTabCompleteOneOf: Boolean =
    annotations.exists(_.isInstanceOf[TabCompleteAsOneOf])
  def isTabComplete: Boolean =
    annotations.exists(_.isInstanceOf[TabCompleter])
  def isCatchInvalidFlags: Boolean =
    annotations.exists(_.isInstanceOf[CatchInvalidFlags])
  def isPositionalArgument: Boolean =
    annotations.exists {
      case ExampleValue(CommandLineParser.PositionalArgument) => true
      case _: PositionalArguments => true
      case _ => false
    }

}

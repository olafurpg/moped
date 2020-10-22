package moped.internal.console

import scala.util.Try

import moped.annotations.Inline
import moped.internal.console.CommandLineParser._
import moped.internal.reporters.Levenshtein
import moped.json._
import moped.macros.ClassShaper
import moped.macros.ParameterShape
import moped.reporters._
import scala.collection.immutable.Nil

class CommandLineParser[T](
    settings: ClassShaper[T],
    toInline: Map[String, ParameterShape]
) {
  private def loop(
      curr: JsonObject,
      xs: List[String],
      s: State
  ): DecodingResult[JsonObject] = {
    (xs, s) match {
      case (Nil, NoFlag) => ValueResult(curr)
      case (Nil, Flag(flag, setting)) =>
        if (setting.isBoolean) ValueResult(add(curr, flag, JsonBoolean(true)))
        else {
          ErrorResult(
            Diagnostic.error(
              s"the argument '--${Cases.camelToKebab(flag)}' requires a value but none was supplied"
            )
          )
        }
      case ("--" :: tail, NoFlag) =>
        val trailingArguments = appendValues(
          curr,
          TrailingArgument,
          tail.map(JsonString(_))
        )
        loop(add(curr, TrailingArgument, trailingArguments), Nil, NoFlag)
      case (head :: tail, NoFlag) =>
        val equal = head.indexOf('=')
        if (equal >= 0) { // split "--key=value" into ["--key", "value"]
          val key = head.substring(0, equal)
          val value = head.substring(equal + 1)
          loop(curr, key :: value :: tail, NoFlag)
        } else if (head.startsWith("-")) {
          tryFlag(curr, head, tail, defaultBooleanValue = true) match {
            case nok: ErrorResult if head.startsWith("--") =>
              val fallbackFlag =
                if (head.startsWith(noPrefix)) {
                  "--" + head.stripPrefix(noPrefix)
                } else {
                  noPrefix + head.stripPrefix("--")
                }
              val fallback =
                tryFlag(
                  curr,
                  fallbackFlag,
                  tail,
                  defaultBooleanValue = false
                )
              fallback.orElse(nok)
            case ok => ok
          }
        } else {
          val positionalArgs =
            appendValues(
              curr,
              PositionalArgument,
              List(JsonString(head))
            )
          loop(add(curr, PositionalArgument, positionalArgs), tail, NoFlag)
        }
      case (head :: tail, Flag(flag, setting)) =>
        val value =
          if (setting.isNumber)
            Try(JsonNumber(head.toDouble)).getOrElse(JsonString(head))
          else JsonString(head)
        val newCurr =
          if (setting.isRepeated) {
            appendValues(curr, flag, List(value))
          } else {
            value
          }
        loop(add(curr, flag, newCurr), tail, NoFlag)
    }
  }

  private def tryFlag(
      curr: JsonObject,
      head: String,
      tail: List[String],
      defaultBooleanValue: Boolean
  ): DecodingResult[JsonObject] = {
    val camel = Cases.kebabToCamel(dash.replaceFirstIn(head, ""))
    camel.split("\\.").toList match {
      case Nil =>
        ErrorResult(Diagnostic.error(s"Flag '$head' must not be empty"))
      case flag :: flags =>
        val (key, keys) = toInline.get(flag) match {
          case Some(setting) => setting.name -> (flag :: flags)
          case _ => flag -> flags
        }
        settings.get(key, keys) match {
          case None =>
            settings.parametersFlat.find(_.isCatchInvalidFlags) match {
              case None =>
                val closestCandidate =
                  Levenshtein.closestCandidate(camel, settings.nonHiddenNames)
                val didYouMean = closestCandidate match {
                  case None =>
                    ""
                  case Some(candidate) =>
                    val kebab = Cases.camelToKebab(candidate)
                    s"\n\tDid you mean '--$kebab'?"
                }
                val kebabFlag = Cases.camelToKebab(flag)
                ErrorResult(
                  Diagnostic.error(
                    s"found argument '--$kebabFlag' which wasn't expected, or isn't valid in this context.$didYouMean"
                  )
                )
              case Some(_) =>
                val values = appendValues(
                  curr,
                  PositionalArgument,
                  (head :: tail).map(JsonString(_))
                )
                ValueResult(add(curr, PositionalArgument, values))
            }
          case Some(setting) =>
            val prefix = toInline.get(flag).fold("")(_.name + ".")
            val keys = toInline.get(flag) match {
              case Some(i) => i.name :: flag :: Nil
              case None => flag :: Nil
            }
            if (setting.isBoolean) {
              val newCurr =
                add(curr, keys, JsonBoolean(defaultBooleanValue))
              loop(newCurr, tail, NoFlag)
            } else {
              loop(curr, tail, Flag(flag, setting))
            }
        }
    }
  }
}

object CommandLineParser {
  val PositionalArgument = "moped@positional"
  val TrailingArgument = "moped@trailing"

  def parseArgs[T](
      args: List[String]
  )(implicit settings: ClassShaper[T]): DecodingResult[JsonObject] = {
    val toInline = inlinedSettings(settings)
    val parser = new CommandLineParser[T](settings, toInline)
    val x = parser.loop(JsonObject(Nil), args, NoFlag)
    x
  }

  private def add(
      curr: JsonObject,
      key: String,
      value: JsonElement
  ): JsonObject = {
    add(curr, List(key), value)
  }

  private def add(
      curr: JsonObject,
      keys: List[String],
      value: JsonElement
  ): JsonObject = {
    def loop(ks: List[String]): JsonMember =
      ks match {
        case Nil => throw new IllegalArgumentException("Nil")
        case head :: Nil =>
          JsonMember(JsonString(head), value)
        case head :: tail =>
          JsonMember(JsonString(head), JsonObject(List(loop(tail))))
      }
    curr + loop(keys)
  }

  val noPrefix = "--no-"
  def isNegatedBoolean(flag: String): Boolean = flag.startsWith(noPrefix)

  def appendValues(
      obj: JsonObject,
      key: String,
      values: List[JsonElement]
  ): JsonArray = {
    obj.getMember(key) match {
      case Some(JsonArray(oldValues)) => JsonArray(oldValues ++ values)
      case _ => JsonArray(values)
    }
  }

  def inlinedSettings(
      settings: ClassShaper[_]
  ): Map[String, ParameterShape] =
    settings.parametersFlat.iterator.flatMap { setting =>
      if (setting.annotations.exists(_.isInstanceOf[Inline])) {
        for {
          underlying <- setting.underlying.toList
          name <- underlying.names
        } yield name -> setting
      } else {
        Nil
      }
    }.toMap

  def allSettings(
      settings: ClassShaper[_]
  ): Map[String, ParameterShape] =
    inlinedSettings(settings) ++ settings.parametersFlat.map(s => s.name -> s)

  private sealed trait State
  private case class Flag(flag: String, setting: ParameterShape) extends State
  private case object NoFlag extends State
  private val dash = "--?".r

}

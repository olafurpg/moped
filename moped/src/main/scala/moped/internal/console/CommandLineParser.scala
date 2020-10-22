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
import scala.collection.mutable

class CommandLineParser[T](
    settings: ClassShaper[T],
    toInline: Map[String, List[InlinedFlag]]
) {
  val merger = new ObjectMergerTraverser()
  val errors = mutable.ListBuffer.empty[Diagnostic]
  def parseArgs(args: List[String]): DecodingResult[JsonObject] = {
    loop(args, NoFlag)
    Diagnostic.fromDiagnostics(errors.toList) match {
      case Some(d) => ErrorResult(d)
      case None =>
        merger.result() match {
          case o: JsonObject =>
            pprint.log(o.toDoc.render(10))
            ValueResult(o)
          case e =>
            ErrorResult(Diagnostic.typeMismatch("Object", DecodingContext(e)))
        }
    }
  }
  private[this] def loop(
      xs: List[String],
      s: State
  ): Unit = {
    (xs, s) match {
      case (Nil, NoFlag) => ()
      case (Nil, Flag(settings)) =>
        settings.foreach { setting =>
          errors += Diagnostic.error(
            s"the argument '--${Cases.camelToKebab(setting.shape.name)}' requires a value but none was supplied"
          )
        }
      case ("--" :: tail, NoFlag) =>
        appendValues(
          TrailingArgument,
          tail.map(JsonString(_))
        )
      case (head :: tail, NoFlag) =>
        val equal = head.indexOf('=')
        if (equal >= 0) { // split "--key=value" into ["--key", "value"]
          val key = head.substring(0, equal)
          val value = head.substring(equal + 1)
          loop(key :: value :: tail, NoFlag)
        } else if (head.startsWith("-")) {
          tryFlag(head, tail, defaultBooleanValue = true)
          // match {
          //   case nok: ErrorResult if head.startsWith("--") =>
          //     val fallbackFlag =
          //       if (head.startsWith(noPrefix)) {
          //         "--" + head.stripPrefix(noPrefix)
          //       } else {
          //         noPrefix + head.stripPrefix("--")
          //       }
          //     val fallback = tryFlag(
          //       fallbackFlag,
          //       tail,
          //       defaultBooleanValue = false
          //     )
          //     fallback.orElse(nok)
          //   case ok => ok
          // }
        } else {
          appendValues(
            PositionalArgument,
            List(JsonString(head))
          )
          loop(tail, NoFlag)
        }
      case (head :: tail, Flag(flags)) =>
        flags.foreach { setting =>
          val value: JsonElement =
            if (setting.shape.isNumber)
              Try(JsonNumber(head.toDouble)).getOrElse(JsonString(head))
            else JsonString(head)
          if (setting.shape.isRepeated) {
            appendValues(setting.keys, List(value))
          } else {
            add(setting.shape.name, value)
            loop(tail, NoFlag)
          }
        }
    }
  }

  private def tryFlag(
      head: String,
      tail: List[String],
      defaultBooleanValue: Boolean
  ): Unit = {
    val camel = Cases.kebabToCamel(dash.replaceFirstIn(head, ""))
    camel.split("\\.").toList match {
      case Nil =>
        errors += Diagnostic.error(s"Flag '$head' must not be empty")
      case flag :: Nil =>
        toInline.get(flag) match {
          case None =>
            settings.parametersFlat.find(_.isCatchInvalidFlags) match {
              case Some(param) =>
                appendValues(
                  param.name,
                  (head :: tail).map(JsonString(_))
                )
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
                errors += Diagnostic.error(
                  s"found argument '--$kebabFlag' which wasn't expected, or isn't valid in this context.$didYouMean"
                )
            }
          case Some(settings) =>
            loopSettings(tail, settings, defaultBooleanValue)
        }
      case flag :: flags =>
        settings.get(flag, flags) match {
          case Some(value) =>
            loopSettings(
              tail,
              List(InlinedFlag(flag :: flags, value)),
              defaultBooleanValue
            )
          case None =>
            ???
        }
    }
  }

  def loopSettings(
      tail: List[String],
      settings: List[InlinedFlag],
      defaultBooleanValue: Boolean
  ): Unit = {
    val hasBoolean = settings.exists(_.shape.isBoolean)
    val isOnlyBoolean = settings.forall(_.shape.isBoolean)
    if (isOnlyBoolean) {
      settings.map { setting =>
        add(setting.keys, JsonBoolean(defaultBooleanValue))
      }
      loop(tail, NoFlag)
    } else {
      if (hasBoolean) {
        errors += Diagnostic.error(
          "either all inlined flags must be boolean or non-boolean"
        )
      } else {
        loop(tail, Flag(settings))
      }
    }
  }

  private def add(
      key: String,
      value: JsonElement
  ): Unit = {
    add(List(key), value)
  }

  private def add(
      keys: List[String],
      value: JsonElement
  ): Unit = {
    merger.mergeMember(newMember(keys, value))
  }

  private def newMember(keys: List[String], value: JsonElement): JsonMember = {
    keys match {
      case Nil => throw new IllegalArgumentException("Nil")
      case head :: Nil =>
        JsonMember(JsonString(head), value)
      case head :: tail =>
        JsonMember(JsonString(head), JsonObject(List(newMember(tail, value))))
    }
  }

  def appendValues(
      key: String,
      values: List[JsonElement]
  ): Unit = {
    appendValues(List(key), values)
  }
  def appendValues(
      keys: List[String],
      values: List[JsonElement]
  ): Unit = {
    merger.mergeMember(newMember(keys, JsonArray(values)))
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
    parser.parseArgs(args)
  }

  val noPrefix = "--no-"
  def isNegatedBoolean(flag: String): Boolean = flag.startsWith(noPrefix)

  def inlinedSettings(
      settings: ClassShaper[_]
  ): Map[String, List[InlinedFlag]] = {
    val buf = mutable.Map.empty[String, mutable.ListBuffer[InlinedFlag]]
    def loop(prefix: List[String], s: ClassShaper[_]): Unit = {
      s.parametersFlat.foreach { param =>
        val lst = buf.getOrElseUpdate(param.name, mutable.ListBuffer.empty)
        lst += InlinedFlag(prefix :+ param.name, param)
      }
      for {
        params <- s.parameters
        param <- params
        if param.isInline
        underlying <- param.underlying.toList
      } {
        loop(prefix :+ param.name, underlying)
      }
    }
    loop(Nil, settings)
    buf.mapValues(_.toList).toMap
  }

  private sealed trait State
  private case class Flag(flags: List[InlinedFlag]) extends State {
    require(flags.nonEmpty)
  }
  private case object NoFlag extends State
  private val dash = "--?".r

  def allSettings(
      settings: ClassShaper[_]
  ): Map[String, List[InlinedFlag]] =
    inlinedSettings(settings) ++
      settings.parametersFlat.iterator.map(s => s.name -> List(InlinedFlag(s)))

}

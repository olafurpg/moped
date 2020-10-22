package moped.internal.console

import scala.collection.immutable.Nil
import scala.collection.mutable
import scala.util.Try

import moped.internal.console.CommandLineParser._
import moped.internal.reporters.Levenshtein
import moped.json._
import moped.macros.ClassShaper
import moped.reporters._

class CommandLineParser[T](
    settings: ClassShaper[T],
    toInline: Map[String, List[InlinedFlag]]
) {
  private val pendingMembers = mutable.ListBuffer.empty[JsonElement]
  private val pendingArrays =
    mutable.Map.empty[List[String], mutable.ListBuffer[JsonElement]]
  private val errors = mutable.ListBuffer.empty[Diagnostic]
  private def flushArrays(): Unit = {
    pendingArrays.foreach {
      case (keys, values) =>
        pushMember(newMember(keys, JsonArray(values.toList)))
    }
    pendingArrays.clear()
  }
  private def pushMember(member: JsonMember): Unit = {
    pendingMembers += JsonObject(List(member))
  }
  def parseArgs(args: List[String]): DecodingResult[JsonObject] = {
    loop(args, NoFlag)
    flushArrays()
    Diagnostic.fromDiagnostics(errors.toList) match {
      case Some(d) => ErrorResult(d)
      case None =>
        JsonElement.merge(pendingMembers) match {
          case o: JsonObject =>
            // pprint.log(o.toDoc.render(10))
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
          loopFlag(head, tail)
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
            addMember(setting.keys, value)
          }
        }
        loop(tail, NoFlag)
    }
  }

  private def loopFlag(
      flag: String,
      tail: List[String]
  ): Unit = {
    tryFlag(flag, tail) match {
      case Left(error) =>
        if (flag.startsWith(negatedPrefix)) {
          // Try to parse flag with "--no-" prefix removed.
          val negatedHead = flag.stripPrefix(negatedPrefix)
          tryFlag(negatedHead, tail) match {
            case Left(_) =>
              errors += error
            case Right(Nil) => // Done.
            case Right(settings) =>
              // --no prefix succeeded, default boolean value is false.
              loopSettings(tail, settings, defaultBooleanValue = false)
          }
        } else {
          errors += error
        }
      case Right(Nil) => // Done.
      case Right(settings) =>
        loopSettings(tail, settings, defaultBooleanValue = true)
    }
  }

  private def tryFlag(
      flag: String,
      tail: List[String]
  ): Either[Diagnostic, List[InlinedFlag]] = {
    val camel = Cases.kebabToCamel(dash.replaceFirstIn(flag, ""))
    camel.split("\\.").toList match {
      case Nil =>
        Left(Diagnostic.error(s"Flag '$flag' must not be empty"))
      case flag :: Nil =>
        toInline.get(flag) match {
          case None =>
            settings.parametersFlat.find(_.isCatchInvalidFlags) match {
              case Some(param) =>
                appendValues(
                  param.name,
                  (flag :: tail).map(JsonString(_))
                )
                Right(Nil)
              case None =>
                Left(didYouMean(flag, camel))
            }
          case Some(settings) =>
            Right(settings)
        }
      case flag :: flags =>
        settings.get(flag, flags) match {
          case Some(value) =>
            Right(List(InlinedFlag(flag :: flags, value)))
          case None =>
            Left(didYouMean(flag, camel))
        }
    }
  }

  def didYouMean(flag: String, camel: String): Diagnostic = {
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
    Diagnostic.error(
      s"found argument '--$kebabFlag' which wasn't expected, or isn't valid in this context.$didYouMean"
    )
  }

  def loopSettings(
      tail: List[String],
      settings: List[InlinedFlag],
      defaultBooleanValue: Boolean
  ): Unit = {
    require(settings.nonEmpty)
    val hasBoolean = settings.exists(_.shape.isBoolean)
    val isOnlyBoolean = settings.forall(_.shape.isBoolean)
    if (isOnlyBoolean) {
      settings.map { setting =>
        addMember(setting.keys, JsonBoolean(defaultBooleanValue))
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

  private def addMember(
      key: String,
      value: JsonElement
  ): Unit = {
    addMember(List(key), value)
  }

  private def addMember(
      keys: List[String],
      value: JsonElement
  ): Unit = {
    pushMember(newMember(keys, value))
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
    val buf = pendingArrays.getOrElseUpdate(keys, mutable.ListBuffer.empty)
    buf ++= values
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

  val negatedPrefix = "--no-"
  def isNegatedBoolean(flag: String): Boolean = flag.startsWith(negatedPrefix)

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

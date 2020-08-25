package moped.internal.console

import moped.annotations.Inline
import moped.internal.reporters.Docs._
import moped.json._
import moped.macros._
import org.typelevel.paiges.Doc
import org.typelevel.paiges.Doc._

object HelpMessage {
  def generateManpage[T](
      default: T
  )(implicit encoder: JsonEncoder[T], settings: ClassShaper[T]): Doc = {
    def toHelp(setting: ParameterShape, value: JsonElement): Doc = {
      val name = "--" + Cases.camelToKebab(setting.name)
      val defaultValue = value match {
        case JsonNull() => ""
        case _ => s" = ${value.toDoc.render(80)}"
      }
      IP + quoted(name) + Doc.line + setting.description.getOrElse(Doc.empty)
    }
    val defaultConf = defaults(default)
    val keyValues = parameters(settings, defaultConf).map {
      case (a, b) =>
        toHelp(a, b)
    }
    Doc.intercalate(Doc.line, keyValues)
  }
  def generate[T](
      default: T
  )(implicit encoder: JsonEncoder[T], settings: ClassShaper[T]): Doc = {
    def toHelp(setting: ParameterShape, value: JsonElement): (String, Doc) = {
      val name = Cases.camelToKebab(setting.name)
      val defaultValue = value match {
        case JsonNull() => ""
        case _ => s" = ${value.toDoc.render(80)}"
      }
      val key = s"--$name: ${setting.tpe}$defaultValue "
      key -> setting.description.getOrElse(Doc.empty)
    }
    val defaultConf = defaults(default)
    val keyValues = parameters(settings, defaultConf).map {
      case (a, b) => toHelp(a, b)
    }
    tabulate(keyValues)
  }

  def defaults[T](
      default: T
  )(implicit
      encoder: JsonEncoder[T],
      settings: ClassShaper[T]
  ): Map[String, JsonElement] = {
    encoder.encode(default) match {
      case obj @ JsonObject(members) => obj.value
      case _ => Map.empty
    }
  }
  private def parameters(
      settings: ClassShaper[_],
      defaultConf: Map[String, JsonElement]
  ): List[(ParameterShape, JsonElement)] = {
    settings.parametersFlat.flatMap { setting =>
      val value = defaultConf.getOrElse(setting.name, JsonNull())
      if (
        setting.isHidden ||
        setting.isPositionalArgument ||
        setting.isTrailingArgument
      ) {
        Nil
      } else if (setting.annotations.exists(_.isInstanceOf[Inline])) {
        for {
          underlying <- setting.underlying.toList
          (field, fieldDefault) <- underlying.parametersFlat.zip(value match {
            case obj: JsonObject => obj.members.map(_.value)
            case _ => List(value)
          })
        } yield (field, fieldDefault)
      } else {
        (setting, value) :: Nil
      }
    }

  }
}

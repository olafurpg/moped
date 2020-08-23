package moped.internal.console

import moped.annotations.Inline
import moped.json._
import moped.macros._
import org.typelevel.paiges.Doc
import org.typelevel.paiges.Doc._

object HelpMessage {
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

    val defaultConf: Map[String, JsonElement] = encoder.encode(default) match {
      case obj @ JsonObject(members) => obj.value
      case _ => Map.empty
    }
    val keyValues = settings.parametersFlat.flatMap { setting =>
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
        } yield toHelp(field, fieldDefault)
      } else {
        toHelp(setting, value) :: Nil
      }
    }
    tabulate(keyValues)
  }
}

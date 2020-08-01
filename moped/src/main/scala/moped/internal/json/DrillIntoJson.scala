package moped.internal.json

import moped.internal.diagnostics.MissingFieldDiagnostic
import moped.internal.diagnostics.TypeMismatchDiagnostic
import moped.json._
import moped.macros.ParameterShape

object DrillIntoJson {
  def getKey(obj: JsonElement, keys: Seq[String]): Option[JsonElement] =
    if (keys.isEmpty) None
    else {
      obj match {
        case obj @ JsonObject(_) =>
          obj.members
            .collectFirst {
              case JsonMember(JsonString(key), value) if key == keys.head =>
                value
            }
            .orElse(getKey(obj, keys.tail))
        case _ => None
      }
    }

  def getOrElse[T](
      conf: JsonElement,
      default: T,
      param: ParameterShape
  )(implicit ev: JsonDecoder[T]): DecodingResult[T] = {
    getKey(conf, param.allNames) match {
      case Some(value) => ev.decode(value)
      case None => ValueResult(default)
    }
  }

  def get[T](obj: JsonObject, path: String, extraNames: String*)(implicit
      ev: JsonDecoder[T]
  ): DecodingResult[T] = {
    get[T](DecodingContext(obj, SelectMemberCursor(path)), path, extraNames: _*)
  }

  def get[T](context: DecodingContext, path: String, extraNames: String*)(
      implicit ev: JsonDecoder[T]
  ): DecodingResult[T] = {
    getKey(context.json, path +: extraNames) match {
      case Some(value) => ev.decode(value)
      case None =>
        context.json match {
          case JsonObject(_) =>
            ErrorResult(new MissingFieldDiagnostic(context))
          case _ =>
            ErrorResult(
              new TypeMismatchDiagnostic(
                s"JsonObject with field $path",
                context
              )
            )
        }
    }
  }

}

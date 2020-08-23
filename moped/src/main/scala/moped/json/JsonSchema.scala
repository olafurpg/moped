package moped.json

import moped.macros.ClassShaper
import moped.macros.ParameterShape
import moped.internal.diagnostics.TypeMismatchDiagnostic
import moped.reporters.NoPosition

object JsonSchema {

  def generate[T: JsonEncoder](
      title: String,
      description: String,
      url: Option[String],
      default: T
  )(implicit settings: ClassShaper[T]): DecodingResult[JsonObject] = {
    JsonEncoder[T].encode(default) match {
      case obj: JsonObject =>
        ValueResult(generate[T](title, description, url, obj))
      case els =>
        ErrorResult(
          new TypeMismatchDiagnostic(
            "JsonObject",
            els.productPrefix,
            NoPosition,
            NoCursor()
          )
        )
    }
  }

  def generate[T](
      title: String,
      description: String,
      url: Option[String],
      default: JsonObject
  )(implicit settings: ClassShaper[T]): JsonObject = {

    val properties = JsonElement.fromMembers(
      settings.parametersFlat
        .zip(default.members)
        .map { case (s, JsonMember(_, v)) => fromSetting(s, v) }: _*
    )

    JsonElement.fromMembers(
      "$id" -> url.map(JsonString).getOrElse(JsonNull()),
      "title" -> JsonString(title),
      "description" -> JsonString(description),
      "type" -> JsonString("object"),
      "properties" -> properties
    )
  }

  private def fromSetting(
      setting: ParameterShape,
      defaultValue: JsonElement
  ): (String, JsonObject) = {
    val obj = JsonElement.fromMembers(
      "title" -> JsonString(setting.name),
      "description" -> setting.description
        .map(desc => JsonString(desc.render(80)))
        .getOrElse(JsonNull()),
      "default" -> defaultValue,
      "required" -> JsonBoolean(false),
      "type" -> toSchemaType(defaultValue)
    )
    val withProperties: JsonObject = defaultValue match {
      case JsonObject(values) =>
        val properties = JsonElement.fromMembers(
          setting.underlying
            .map(
              _.parametersFlat
                .zip(values)
                .map { case (s, JsonMember(_, v)) => fromSetting(s, v) }
            )
            .getOrElse(Nil): _*
        )
        obj + JsonMember(JsonString("properties"), properties)
      case _ =>
        obj
    }
    setting.name -> withProperties
  }

  private def toSchemaType(conf: JsonElement): JsonString =
    JsonString(
      conf match {
        // https://tools.ietf.org/html/draft-handrews-json-schema-01#section-4.2.1
        case _: JsonBoolean => "boolean"
        case _: JsonNumber => "number"
        case _: JsonArray => "array"
        case _: JsonString => "string"
        case _: JsonNull => "null"
        case _: JsonObject => "object"
      }
    )

}

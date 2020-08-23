package moped.json

import scala.collection.immutable.ListMap

import moped.internal.transformers.JsonTransformer
import moped.reporters._
import org.typelevel.paiges.Doc
import ujson.StringRenderer

sealed abstract class JsonElement extends Product with Serializable {
  private var myPosition: Position = NoPosition
  def position = myPosition
  def withPosition(newPosition: Position): JsonElement = {
    if (myPosition.isNone && newPosition.isNone) this
    else {
      val copy = copyThis()
      copy.myPosition = newPosition
      copy
    }
  }

  def isArray: Boolean = this.isInstanceOf[JsonArray]
  def isObject: Boolean = this.isInstanceOf[JsonObject]
  def isString: Boolean = this.isInstanceOf[JsonString]
  def isBoolean: Boolean = this.isInstanceOf[JsonBoolean]
  def isNumber: Boolean = this.isInstanceOf[JsonNumber]
  def isNull: Boolean = this.isInstanceOf[JsonNull]
  def isPrimitive: Boolean = this.isInstanceOf[JsonPrimitive]

  private def copyThis(): JsonElement =
    this match {
      case JsonNull() => JsonNull()
      case JsonNumber(value) => JsonNumber(value)
      case JsonBoolean(value) => JsonBoolean(value)
      case JsonString(value) => JsonString(value)
      case JsonArray(value) => JsonArray(value)
      case JsonObject(value) => JsonObject(value)
    }
  final def toDoc: Doc =
    this match {
      case JsonNull() => Doc.text("null")
      case JsonNumber(value) =>
        Doc.text(JsonTransformer.transform(this, StringRenderer()).toString())
      case JsonBoolean(value) => Doc.text(value.toString())
      case JsonString(value) =>
        Doc.text(JsonTransformer.transform(this, StringRenderer()).toString())
      case JsonArray(elements) =>
        if (elements.isEmpty) {
          Doc.text("[]")
        } else {
          val parts = Doc.intercalate(
            Doc.comma + Doc.lineOrSpace,
            elements.map(_.toDoc)
          )
          parts.bracketBy(Doc.text("["), Doc.text("]"))
        }
      case obj @ JsonObject(members) =>
        val keyValues = obj.value.map {
          case (s, j) =>
            JsonString(s).toDoc + Doc.text(":") +
              Doc.space + j.toDoc
        }
        val parts = Doc.intercalate(Doc.comma + Doc.line, keyValues)
        parts.bracketBy(Doc.text("{"), Doc.text("}"))
    }
}

object JsonElement {
  def merge(elements: Iterable[JsonElement]): JsonElement = {
    if (elements.hasDefiniteSize && elements.size == 1) {
      // TODO(olafur): figure out why this special case is needed
      elements.head
    } else {
      val merger = new ObjectMergerTraverser()
      elements.foreach { elem =>
        merger.mergeElement(elem)
      }
      merger.result()
    }
  }
}
sealed abstract class JsonPrimitive extends JsonElement
final case class JsonNull() extends JsonPrimitive
final case class JsonNumber(value: Double) extends JsonPrimitive
final case class JsonBoolean(value: Boolean) extends JsonPrimitive
final case class JsonString(value: String) extends JsonPrimitive
final case class JsonArray(elements: List[JsonElement]) extends JsonElement
final case class JsonObject(members: List[JsonMember]) extends JsonElement {
  val value: ListMap[String, JsonElement] =
    ListMap(members.map(m => m.key.value -> m.value): _*)
  def getMember(key: String): Option[JsonElement] = {
    value.get(key)
  }
}
final case class JsonMember(key: JsonString, value: JsonElement)

package moped.json

import scala.collection.immutable.ListMap

import moped.reporters._
import org.typelevel.paiges.Doc
import ujson.AstTransformer
import upickle.core.Visitor
import upickle.core.ArrVisitor
import upickle.core.ObjVisitor
import scala.collection.mutable
import upickle.core.Util
import ujson.StringRenderer

sealed abstract class JsonElement extends Product with Serializable {
  private var myPosition: Position = NoPosition
  def position = myPosition
  def withPosition(newPosition: Position): JsonElement = {
    val copy = copyThis()
    copy.myPosition = newPosition
    copy
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
        Doc.text(JsonElement.transform(this, StringRenderer()).toString())
      case JsonBoolean(value) => Doc.text(value.toString())
      case JsonString(value) =>
        Doc.text(JsonElement.transform(this, StringRenderer()).toString())
      case JsonArray(elements) =>
        if (elements.isEmpty) {
          Doc.text("[]")
        } else {
          val parts = Doc.intercalate(
            Doc.comma,
            elements.map { j =>
              (Doc.line + j.toDoc).grouped
            }
          )
          "[" +: ((parts :+ " ]").nested(2))
        }
      case obj @ JsonObject(members) =>
        val keyValues = obj.value.map {
          case (s, j) =>
            JsonString(s).toDoc + Doc.text(":") + ((Doc.lineOrSpace + j.toDoc)
              .nested(2))
        }
        val parts = Doc.fill(Doc.comma, keyValues)
        parts.bracketBy(Doc.text("{"), Doc.text("}"))
    }
}

object JsonElement extends AstTransformer[JsonElement] {
  def merge(elements: Iterable[JsonElement]): JsonElement = {
    val merger = new ObjectMergerTraverser()
    elements.foreach { elem =>
      merger.mergeElement(elem)
    }
    merger.result()
  }
  def transform[T](j: JsonElement, f: Visitor[_, T]): T =
    j match {
      case JsonNull() => f.visitNull(-1)
      case JsonNumber(value) => f.visitFloat64(value, -1)
      case JsonBoolean(true) => f.visitTrue(-1)
      case JsonBoolean(false) => f.visitFalse(-1)
      case JsonString(value) => f.visitString(value, -1)
      case JsonArray(elements) => transformArray(f, elements)
      case obj @ JsonObject(members) => transformObject(f, obj.value)
    }
  def visitArray(
      length: Int,
      index: Int
  ): ArrVisitor[JsonElement, JsonElement] =
    new AstArrVisitor[mutable.ListBuffer](buf => JsonArray(buf.toList))
  def visitObject(
      length: Int,
      index: Int
  ): ObjVisitor[JsonElement, JsonElement] =
    new AstObjVisitor[mutable.ListBuffer[(String, JsonElement)]](buf =>
      JsonObject(buf.iterator.map {
        case (key, value) => JsonMember(JsonString(key), value)
      }.toList)
    )
  def visitNull(index: Int): JsonElement = JsonNull()
  def visitFalse(index: Int): JsonElement = JsonBoolean(false)
  def visitTrue(index: Int): JsonElement = JsonBoolean(true)
  def visitFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): JsonElement =
    JsonNumber(
      if (decIndex != -1 || expIndex != -1) s.toString.toDouble
      else Util.parseIntegralNum(s, decIndex, expIndex, index)
    )
  def visitString(s: CharSequence, index: Int): JsonElement =
    JsonString(s.toString())
}
sealed abstract class JsonPrimitive extends JsonElement
final case class JsonNull() extends JsonPrimitive
final case class JsonNumber(value: Double) extends JsonPrimitive
final case class JsonBoolean(value: Boolean) extends JsonPrimitive
final case class JsonString(value: String) extends JsonPrimitive
final case class JsonArray(elements: List[JsonElement]) extends JsonElement
final case class JsonObject(members: List[JsonMember]) extends JsonElement {
  val value: Map[String, JsonElement] =
    ListMap(members.map(m => m.key.value -> m.value): _*)
  def getMember(key: String): Option[JsonElement] = {
    value.get(key)
  }
}
final case class JsonMember(key: JsonString, value: JsonElement)

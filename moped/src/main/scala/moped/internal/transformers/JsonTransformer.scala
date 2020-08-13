package moped.internal.transformers

import scala.collection.mutable

import moped.json._
import ujson.AstTransformer
import upickle.core.ArrVisitor
import upickle.core.ObjVisitor
import upickle.core.Util
import upickle.core.Visitor

object JsonTransformer extends AstTransformer[JsonElement] {
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

package moped.internal.transformers

import scala.collection.mutable

import moped.json._
import moped.reporters.Input
import moped.reporters.Position
import ujson.AstTransformer
import upickle.core.ArrVisitor
import upickle.core.ObjVisitor
import upickle.core.Visitor
import scala.reflect.internal.Depth

object JsonTransformer extends JsonTransformer(Input.none)
class JsonTransformer(input: Input)
    extends AstTransformer[JsonElement]
    with TransformerUtils[JsonElement] {
  def pos(index: Int): Position = Position.offset(input, index)
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
    new AstArrVisitor[mutable.ListBuffer](buf =>
      JsonArray(buf.toList).withPosition(Position.offset(input, index))
    )

  def visitObject(
      length: Int,
      index: Int
  ): ObjVisitor[JsonElement, JsonElement] =
    new AstMopedObjectVisitor[mutable.ListBuffer[(JsonString, JsonElement)]](
      buf =>
        JsonObject(buf.iterator.map {
          case (key, value) => JsonMember(key, value)
        }.toList).withPosition(pos(index))
    )
  def visitNull(index: Int): JsonElement = JsonNull().withPosition(pos(index))
  def visitFalse(index: Int): JsonElement =
    JsonBoolean(false).withPosition(pos(index))
  def visitTrue(index: Int): JsonElement =
    JsonBoolean(true).withPosition(pos(index))
  def visitFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): JsonElement =
    JsonNumber(
      parseFloat64StringParts(s, decIndex, expIndex, index)
    ).withPosition(pos(index))
  def visitString(s: CharSequence, index: Int): JsonElement =
    JsonString(s.toString()).withPosition(pos(index))
}

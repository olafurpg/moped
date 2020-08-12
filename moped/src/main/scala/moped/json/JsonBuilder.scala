package moped.json

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

sealed abstract class JsonBuilder {
  def addObjectMember(member: JsonMember): Unit
  def addArrayValue(value: JsonElement): Unit
  def result(): JsonElement
}

object JsonBuilder {
  def apply(): JsonBuilder = new ObjectBuilder()
  def apply(elem: JsonElement): JsonBuilder =
    elem match {
      case e: JsonObject =>
        val result = new ObjectBuilder()
        e.members.foreach(result.addObjectMember(_))
        result
      case e: JsonPrimitive => new PrimitiveBuilder(e)
      case e: JsonArray =>
        val result = new ArrayBuilder()
        e.elements.foreach(result.addArrayValue(_))
        result
    }
}

final class PrimitiveBuilder(value: JsonElement) extends JsonBuilder {
  def addObjectMember(member: JsonMember): Unit = ()
  def addArrayValue(value: JsonElement): Unit = ()
  def result(): JsonElement = value
}

final class ArrayBuilder() extends JsonBuilder {
  private val buf = ListBuffer.empty[JsonElement]
  def addObjectMember(member: JsonMember): Unit = ()
  def addArrayValue(value: JsonElement): Unit = buf += value
  def result(): JsonElement = JsonArray(buf.toList)
}

final class ObjectBuilder() extends JsonBuilder {
  private val buf = mutable.LinkedHashMap.empty[JsonString, JsonBuilder]
  def addObjectMember(member: JsonMember): Unit = {
    (buf.get(member.key), member.value) match {
      case (Some(builder: ObjectBuilder), obj: JsonObject) =>
        obj.members.foreach { m =>
          builder.addObjectMember(m)
        }
      case _ =>
        buf(member.key) = JsonBuilder(member.value)
    }
  }
  def addArrayValue(value: JsonElement): Unit = ()
  def result(): JsonElement =
    JsonObject(
      buf.iterator.map {
        case (k, v) => JsonMember(k, v.result())
      }.toList
    )
}

// final class MemberBuilder() {
//   private val buf = mutable.LinkedHashMap.empty[JsonString, JsonBuilder]
//   def toList: List[JsonMember] =
//     buf.iterator.map {
//       case (k, v) => JsonMember(k, v.result())
//     }.toList
//   def addMember(member: JsonMember): Unit = {
//     val builder = buf.getOrElseUpdate(member.key, JsonBuilder())
//     builder.addValue(member.value)
//   }
// }

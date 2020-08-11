package moped.json

class JsonTraverser {
  def traverse(e: JsonElement): Unit =
    traverse(e, NoCursor())
  def traverse(e: JsonElement, cursor: Cursor): Unit =
    e match {
      case e: JsonPrimitive => traversePrimitive(e, cursor)
      case e: JsonArray => traverseArray(e, cursor)
      case e: JsonObject => traverseObject(e, cursor)
    }
  def traversePrimitive(e: JsonPrimitive, cursor: Cursor): Unit = {
    e match {
      case e: JsonNull => traverseNull(e, cursor)
      case e: JsonNumber => traverseNumber(e, cursor)
      case e: JsonBoolean => traverseBoolean(e, cursor)
      case e: JsonString => traverseString(e, cursor)
    }
  }
  def traverseNull(e: JsonNull, cursor: Cursor): Unit = ()
  def traverseNumber(e: JsonNumber, cursor: Cursor): Unit = ()
  def traverseBoolean(e: JsonBoolean, cursor: Cursor): Unit = ()
  def traverseString(e: JsonString, cursor: Cursor): Unit = ()
  def traverseArray(e: JsonArray, cursor: Cursor): Unit = {
    e.elements.zipWithIndex.foreach {
      case (e, i) =>
        traverse(e, SelectIndexCursor(i).withParent(cursor))
    }
  }
  def traverseObjectMember(e: JsonMember, cursor: Cursor): Unit = {
    traverseString(e.key, cursor)
    traverse(e.value, SelectMemberCursor(e.key.value).withParent(cursor))
  }
  def traverseObject(e: JsonObject, cursor: Cursor): Unit = {
    e.members.foreach { m =>
      traverseObjectMember(m, cursor)
    }
  }
}

package moped.json

import java.{util => ju}

class JsonMerger extends JsonTraverser {
  val stack = new ju.ArrayDeque[JsonBuilder]
  private var isReuseBuilder: Boolean = true
  def mergeElement(elem: JsonElement): Unit = {
    isReuseBuilder = !stack.isEmpty() && stack.getFirst().matches(elem)
    super.traverse(elem)
  }
  def result(): JsonElement = {
    // pprint.log(stack)
    if (stack.isEmpty()) JsonObject(Nil)
    else stack.pop().result()
  }
  override def traversePrimitive(e: JsonPrimitive, cursor: Cursor): Unit = {
    // if (!stack.isEmpty()) {
    //   stack.pop()
    // }
    stack.push(new PrimitiveBuilder(e))
  }
  override def traverseObject(e: JsonObject, cursor: Cursor): Unit = {
    if (isReuseBuilder) {
      isReuseBuilder = false
    } else {
      stack.push(new ObjectBuilder())
    }
    super.traverseObject(e, cursor)
    popStack()
  }
  override def traverseMember(e: JsonMember, cursor: Cursor): Unit = {
    stack.peek.addObjectMember(e)
    super.traverseMember(e, cursor)
  }
  override def traverseArray(e: JsonArray, cursor: Cursor): Unit = {
    if (isReuseBuilder) {
      isReuseBuilder = false
    } else {
      stack.push(new ArrayBuilder())
    }
    super.traverseArray(e, cursor)
    popStack()
  }

  private def popStack(): Unit = {
    val current = stack.pop()
    if (stack.isEmpty()) {
      stack.push(current)
    } else {
      stack.peek.addArrayValue(current.result())
    }
  }
}

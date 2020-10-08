package moped.internal.transformers

import scala.collection.compat._

import moped.json.JsonElement
import moped.json.JsonString
import moped.reporters.Position
import ujson.AstTransformer
import upickle.core.ObjVisitor
import upickle.core.SimpleVisitor
import upickle.core.Util
import upickle.core.Visitor

trait TransformerUtils[I] { outer: AstTransformer[I] =>
  def pos(index: Int): Position
  def parseFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): Double =
    if (decIndex != -1 || expIndex != -1) s.toString.toDouble
    else Util.parseIntegralNum(s, decIndex, expIndex, index)

  def transformObjectWithPositionedKeys[T](
      f: Visitor[_, T],
      items: Iterable[(JsonString, I)]
  ) = {
    val ctx = f.visitObject(items.size, -1).narrow
    for ((key, value) <- items) {
      val keyVisitor = ctx.visitKey(-1)
      ctx.visitKeyValue(keyVisitor.visitString(key.value, key.position.start))
      ctx.visitValue(transform(value, ctx.subVisitor), -1)
    }
    ctx.visitEnd(-1)
  }

  class AstMopedObjectVisitor[T](build: T => I)(implicit
      factory: Factory[(JsonString, I), T]
  ) extends ObjVisitor[I, I] {

    private[this] var key: JsonString = null
    private[this] val vs = factory.newBuilder
    def subVisitor = outer
    def visitKey(index: Int) = JsonStringVisitor
    def visitKeyValue(s: Any): Unit =
      s match {
        case s: JsonString => key = s
        case _ => key = JsonString(s.toString)
      }

    def visitValue(v: I, index: Int): Unit = vs += (key -> v)

    def visitEnd(index: Int): I = build(vs.result)
  }
  object JsonStringVisitor extends SimpleVisitor[Nothing, Any] {
    def expectedMsg = "expected string"
    override def visitString(s: CharSequence, index: Int): JsonElement =
      JsonString(s.toString()).withPosition(pos(index))
  }
}

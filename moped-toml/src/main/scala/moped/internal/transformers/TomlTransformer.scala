package moped.internal.transformers

import moped.reporters.Input
import toml.Value
import ujson.AstTransformer
import upickle.core.ArrVisitor
import upickle.core.ObjVisitor
import upickle.core.Util
import upickle.core.Visitor

object TomlTransformer extends TomlTransformer(Input.none)
class TomlTransformer(input: Input) extends AstTransformer[toml.Value] {
  override def transform[T](j: Value, f: Visitor[_, T]): T =
    j match {
      case Value.Str(value) => f.visitString(value, -1)
      case Value.Bool(true) => f.visitTrue(-1)
      case Value.Bool(false) => f.visitFalse(-1)
      case Value.Real(value) => f.visitFloat64(value, -1)
      case Value.Num(value) => f.visitInt64(value, -1)
      case Value.Tbl(value) => transformObject(f, value)
      case Value.Arr(value) => transformArray(f, value)
    }
  override def visitArray(length: Int, index: Int): ArrVisitor[Value, Value] =
    new AstArrVisitor[List](Value.Arr(_))
  override def visitObject(length: Int, index: Int): ObjVisitor[Value, Value] =
    new AstObjVisitor[Map[String, Value]](Value.Tbl(_))
  override def visitNull(index: Int): Value =
    Value.Str(
      "null"
    ) // Null isn't supported in TOML so we convert into a String.
  override def visitFalse(index: Int): Value = Value.Bool(false)
  override def visitTrue(index: Int): Value = Value.Bool(true)
  override def visitFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): Value =
    Value.Real(
      if (decIndex != -1 || expIndex != -1) s.toString.toDouble
      else Util.parseIntegralNum(s, decIndex, expIndex, index)
    )
  override def visitString(s: CharSequence, index: Int): Value =
    Value.Str(s.toString())
}

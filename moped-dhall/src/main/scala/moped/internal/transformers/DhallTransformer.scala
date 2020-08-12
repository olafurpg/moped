package moped.internal.transformers

import org.dhallj.ast
import org.dhallj.core.Expr
import ujson.AstTransformer
import upickle.core.ArrVisitor
import upickle.core.ObjVisitor
import upickle.core.Util
import upickle.core.Visitor

object DhallTransformer extends AstTransformer[Expr] {
  def transform[T](j: Expr, f: Visitor[_, T]): T = {
    j match {
      case ast.BoolLiteral(c) => if (c) f.visitTrue(-1) else f.visitFalse(-1)
      case ast.DoubleLiteral(c) => f.visitFloat64(c, -1)
      case ast.TextLiteral(c, _) => f.visitString(c, -1)
      // case ast.NullLiteral(c) => f.visitNull(c, -1) // Does not exist
      case ast.ListLiteral(c) => transformArray(f, c)
      case ast.RecordLiteral(c) => transformObject(f, c)
    }
  }
  def visitArray(length: Int, index: Int): ArrVisitor[Expr, Expr] =
    new AstArrVisitor[List](lst => Expr.makeNonEmptyListLiteral(lst.toArray))
  def visitObject(length: Int, index: Int): ObjVisitor[Expr, Expr] =
    new AstObjVisitor[Map[String, Expr]](ast.RecordLiteral(_))
  def visitNull(index: Int): Expr =
    ast.TextLiteral("null") // does not exist in Dhall
  def visitFalse(index: Int): Expr = ast.BoolLiteral(false)
  def visitTrue(index: Int): Expr = ast.BoolLiteral(true)
  def visitFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): Expr =
    ast.DoubleLiteral(
      if (decIndex != -1 || expIndex != -1) s.toString.toDouble
      else Util.parseIntegralNum(s, decIndex, expIndex, index)
    )
  def visitString(s: CharSequence, index: Int): Expr =
    ast.TextLiteral(s.toString())
}

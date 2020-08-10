package moped.myaml

import ujson.AstTransformer
import upickle.core.Visitor
import upickle.core.ArrVisitor
import upickle.core.ObjVisitor
import java.{util => ju}
import scala.collection.JavaConverters._
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import upickle.core.Util

class YamlElement(val value: Any)
object YamlElement extends AstTransformer[YamlElement] {
  override def transform[T](j: YamlElement, f: Visitor[_, T]): T = {
    j.value match {
      case c: ju.Map[_, _] =>
        transformObject(
          f,
          c.asScala.collect {
            case (k: String, v) => k -> new YamlElement(v)
          }
        )
      case c: ju.List[_] =>
        transformArray(
          f,
          c.asScala.map(v => new YamlElement(v))
        )
      case java.lang.Boolean.TRUE => f.visitTrue(-1)
      case java.lang.Boolean.FALSE => f.visitFalse(-1)
      case c: java.lang.String => f.visitString(c, -1)
      case c: java.lang.Integer => f.visitInt32(c, -1)
      case c: java.lang.Double => f.visitFloat64(c, -1)
      case c: java.lang.Float => f.visitFloat32(c, -1)
      case c: java.lang.Long => f.visitInt64(c, -1)
      case c: Array[Byte] =>
        f.visitString(
          new String(ju.Base64.getEncoder().encode(c), StandardCharsets.UTF_8),
          -1
        )
      case c: java.math.BigInteger => f.visitFloat64(c.doubleValue(), -1)
      case c @ (_: java.util.Date | _: java.sql.Timestamp | _: java.sql.Date) =>
        f.visitString(c.toString(), -1)
      case null => f.visitNull(-1)
      case x => throw new IllegalArgumentException(s"Unexpected value $x")
    }
  }
  override def visitArray(
      length: Int,
      index: Int
  ): ArrVisitor[YamlElement, YamlElement] =
    new AstArrVisitor[mutable.ListBuffer](buf => new YamlElement(buf.asJava))
  override def visitObject(
      length: Int,
      index: Int
  ): ObjVisitor[YamlElement, YamlElement] =
    new AstObjVisitor[mutable.LinkedHashMap[String, YamlElement]](map =>
      new YamlElement(map)
    )
  override def visitNull(index: Int): YamlElement = new YamlElement(null)
  override def visitFalse(index: Int): YamlElement =
    new YamlElement(java.lang.Boolean.FALSE)
  override def visitTrue(index: Int): YamlElement =
    new YamlElement(java.lang.Boolean.TRUE)
  override def visitFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): YamlElement =
    new YamlElement(
      if (decIndex != -1 || expIndex != -1) s.toString.toDouble
      else Util.parseIntegralNum(s, decIndex, expIndex, index)
    )
  override def visitString(s: CharSequence, index: Int): YamlElement =
    new YamlElement(s.toString())
}

package org.ekrich.config.impl

import ujson.AstTransformer

import org.ekrich.config.ConfigValue
import upickle.core.Visitor
import upickle.core.ArrVisitor
import upickle.core.ObjVisitor
import upickle.core.Util
import scala.collection.mutable
import scala.collection.JavaConverters._
import org.ekrich.config.ConfigObject
import org.ekrich.config.ConfigList

class SconfigTransformer extends AstTransformer[ConfigValue] {
  val origin = SimpleConfigOrigin.newSimple("origin")
  override def transform[T](j: ConfigValue, f: Visitor[_, T]): T = {
    j match {
      case c: ConfigObject => transformObject(f, c.asScala)
      case c: ConfigList => transformArray(f, c.asScala)
      case o =>
        o.unwrapped match {
          case java.lang.Boolean.TRUE => f.visitTrue(-1)
          case java.lang.Boolean.FALSE => f.visitFalse(-1)
          case c: java.lang.String => f.visitString(c, -1)
          case c: java.lang.Integer => f.visitInt32(c, -1)
          case c: java.lang.Double => f.visitFloat64(c, -1)
          case c: java.lang.Long => f.visitInt64(c, -1)
          case null => f.visitNull(-1)
          case x =>
            throw new IllegalArgumentException(
              s"Unexpected config value $j with unwrapped value $x"
            )
        }
    }
  }
  override def visitArray(
      length: Int,
      index: Int
  ): ArrVisitor[ConfigValue, ConfigValue] =
    new AstArrVisitor[mutable.ListBuffer](buf =>
      new SimpleConfigList(
        origin,
        buf.iterator.collect { case a: AbstractConfigValue => a }.toList.asJava
      )
    )
  override def visitObject(
      length: Int,
      index: Int
  ): ObjVisitor[ConfigValue, ConfigValue] =
    new AstObjVisitor[mutable.LinkedHashMap[String, ConfigValue]](map =>
      new SimpleConfigObject(
        origin,
        map.collect { case (k, v: AbstractConfigValue) => (k, v) }.asJava
      )
    )
  override def visitNull(index: Int): ConfigValue = new ConfigNull(origin)
  override def visitFalse(index: Int): ConfigValue =
    new ConfigBoolean(origin, false)
  override def visitTrue(index: Int): ConfigValue =
    new ConfigBoolean(origin, true)
  override def visitFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): ConfigValue =
    ConfigNumber.newNumber(
      origin,
      if (decIndex != -1 || expIndex != -1) s.toString.toDouble
      else Util.parseIntegralNum(s, decIndex, expIndex, index),
      s.toString()
    )
  override def visitString(s: CharSequence, index: Int): ConfigValue =
    new ConfigString.Quoted(origin, s.toString())

}

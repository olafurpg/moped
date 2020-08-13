package moped.internal.transformers

import java.nio.charset.StandardCharsets
import java.{util => ju}

import scala.collection.JavaConverters._

import moped.reporters.Input
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode
import ujson.AstTransformer
import upickle.core.ArrVisitor
import upickle.core.ObjVisitor
import upickle.core.Visitor

class YamlNodeTransformer(input: Input) extends AstTransformer[Node] {
  class YamlNodeConstructor extends Constructor() {
    def transformNode(n: Node): Object = constructObject(n)
  }
  val constructor = new YamlNodeConstructor()
  override def transform[T](j: Node, f: Visitor[_, T]): T = {
    val offset =
      if (
        j.getStartMark() != null &&
        j.getStartMark().getIndex() >= 0 &&
        !input.isEmpty
      ) {
        j.getStartMark().getIndex()
      } else {
        -1
      }
    j match {
      case n: MappingNode =>
        transformObject(
          f,
          n.getValue().asScala.map { t =>
            val key = t.getKeyNode() match {
              case s: ScalarNode => s.getValue()
              case o => o.toString()
            }
            (key, t.getValueNode())
          }
        )
      case n: SequenceNode =>
        transformArray(f, n.getValue().asScala)
      case n: ScalarNode =>
        constructor.transformNode(n) match {
          case java.lang.Boolean.TRUE => f.visitTrue(offset)
          case java.lang.Boolean.FALSE => f.visitFalse(offset)
          case c: java.lang.String => f.visitString(c, offset)
          case c: java.lang.Integer => f.visitInt32(c, offset)
          case c: java.lang.Double => f.visitFloat64(c, offset)
          case c: java.lang.Float => f.visitFloat32(c, offset)
          case c: java.lang.Long => f.visitInt64(c, offset)
          case c: Array[Byte] =>
            f.visitString(
              new String(
                ju.Base64.getEncoder().encode(c),
                StandardCharsets.UTF_8
              ),
              offset
            )
          case c: java.math.BigInteger =>
            f.visitFloat64(c.doubleValue(), offset)
          case c @ (_: java.util.Date | _: java.sql.Timestamp |
              _: java.sql.Date) =>
            f.visitString(c.toString(), offset)
          case null => f.visitNull(offset)
          case x => throw new IllegalArgumentException(s"Unexpected value $x")
        }
    }
  }
  override def visitArray(length: Int, index: Int): ArrVisitor[Node, Node] = ???
  override def visitObject(length: Int, index: Int): ObjVisitor[Node, Node] =
    ???
  override def visitNull(index: Int): Node = ???
  override def visitFalse(index: Int): Node = ???
  override def visitTrue(index: Int): Node = ???
  override def visitFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): Node = ???
  override def visitString(s: CharSequence, index: Int): Node = ???
}

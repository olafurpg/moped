package moped.macros

import moped.annotations.Description
import moped.annotations.DescriptionDoc
import moped.annotations.ExampleUsage
import moped.annotations.LongDescription
import moped.annotations.LongDescriptionDoc
import moped.annotations.Usage
import moped.internal.console.HelpMessage
import moped.json._
import org.typelevel.paiges.Doc

object ClassShaper {
  def empty[T]: ClassShaper[T] =
    ClassShaper[T](ClassShape.empty)
  def apply[T](s: ClassShape): ClassShaper[T] =
    new ClassShaper[T] { def shape: ClassShape = s }
}

trait ClassShaper[T] extends Product {
  def shape: ClassShape

  def parameters = shape.parameters
  def parametersFlat = parameters.flatten

  def annotations = shape.annotations

  def names: List[String] = parametersFlat.map(_.name)
  def nonHiddenNames: List[String] =
    parametersFlat.filterNot(_.isHidden).map(_.name)
  def allNames: List[String] =
    for {
      setting <- parametersFlat
      name <- setting.allNames
    } yield name

  def get(name: String): Option[ParameterShape] =
    parametersFlat.find(_.matchesLowercase(name))

  def get(name: String, rest: List[String]): Option[ParameterShape] =
    get(name).flatMap { setting =>
      if (setting.isDynamic) {
        Some(setting)
      } else {
        rest match {
          case Nil => Some(setting)
          case head :: tail =>
            for {
              underlying <- setting.underlying
              next <- underlying.get(head, tail)
            } yield next
        }
      }
    }

  def commandLineHelp(
      default: T
  )(implicit ev: JsonEncoder[T]): String =
    commandLineHelp(default, 80)

  def commandLineHelp(
      default: T,
      width: Int
  )(implicit ev: JsonEncoder[T]): String =
    HelpMessage.generate[T](default)(ev, this).renderTrim(width)

  def commandLineDescription: Option[Doc] =
    annotations.collectFirst {
      case DescriptionDoc(doc) => doc
      case Description(doc) => Doc.text(doc)
    }

  def commandLineLongDescription: Option[Doc] =
    annotations.collectFirst {
      case LongDescriptionDoc(doc) => doc
      case LongDescription(doc) => Doc.text(doc)
    }

  def commandLineUsage: Option[Doc] =
    annotations.collectFirst {
      case Usage(doc) => Doc.text(doc)
    }

  def commandLineExamples: List[Doc] =
    annotations.collect {
      case ExampleUsage(example) => Doc.text(example)
    }

  // Product methods, these exists to make `pprint.log()` print readable output.
  override def productArity: Int = 1
  override def canEqual(that: Any): Boolean = that.isInstanceOf[ClassShaper[_]]
  override def productElement(n: Int): Any =
    if (n == 0) shape else throw new NoSuchElementException(n.toString())
  override def productPrefix: String = "ClassShaper"
  override def toString: String = pprint.tokenize(this).mkString

}

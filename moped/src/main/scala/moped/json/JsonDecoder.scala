package moped.json

import moped.internal.diagnostics.TypeMismatchDiagnostic
import moped.reporters.Diagnostic
import scala.collection.compat._
import scala.collection.mutable
import java.nio.file.Path
import java.nio.file.Paths
import scala.reflect.ClassTag

trait JsonDecoder[A] { self =>

  def decode(context: DecodingContext): DecodingResult[A]
  final def decode(elem: JsonElement): DecodingResult[A] =
    decode(DecodingContext(elem))

  final def map[B](fn: A => B): JsonDecoder[B] =
    context => decode(context).map(fn)

  final def flatMap[B](fn: A => DecodingResult[B]): JsonDecoder[B] =
    context => decode(context).flatMap(fn)

}

object JsonDecoder {

  def apply[A](implicit ev: JsonDecoder[A]): JsonDecoder[A] =
    ev
  def constant[A](value: A): JsonDecoder[A] =
    _ => ValueResult(value)

  def fromJson[A](expected: String)(
      fn: PartialFunction[JsonElement, DecodingResult[A]]
  ): JsonDecoder[A] = { context =>
    fn.applyOrElse[JsonElement, DecodingResult[A]](
      context.json,
      _ => ErrorResult(new TypeMismatchDiagnostic(expected, context))
    )
  }
  implicit val jsonElementJsonDecoder: JsonDecoder[JsonElement] =
    context => ValueResult(context.json)
  implicit val intJsonDecoder: JsonDecoder[Int] =
    fromJson[Int]("JsonNumber") {
      case JsonNumber(value) => ValueResult(value.toInt)
    }
  implicit val doubleJsonDecoder: JsonDecoder[Double] =
    fromJson[Double]("JsonNumber") {
      case JsonNumber(value) => ValueResult(value)
    }
  implicit val stringJsonDecoder: JsonDecoder[String] =
    fromJson[String]("JsonString") {
      case JsonString(value) => ValueResult(value)
    }
  implicit val booleanJsonDecoder: JsonDecoder[Boolean] =
    fromJson[Boolean]("JsonBoolean") {
      case JsonBoolean(value) => ValueResult(value)
    }
  implicit val unitJsonDecoder: JsonDecoder[Unit] =
    constant(())
  implicit lazy val pathJsonDecoder: JsonDecoder[Path] =
    stringJsonDecoder.flatMap(path =>
      DecodingResult.fromUnsafe(() => Paths.get(path))
    )

  implicit def arrayJsonDecoder[C[_], A](implicit
      ev: JsonDecoder[A],
      factory: Factory[A, C[A]]
  ): JsonDecoder[C[A]] = { context =>
    context.json match {
      case JsonArray(value) =>
        val successValues = factory.newBuilder
        val errors = List.newBuilder[Diagnostic]
        successValues.sizeHint(value.length)
        value.zipWithIndex.foreach {
          case (value, i) =>
            val cursor = SelectIndexCursor(i).withParent(context.cursor)
            ev.decode(DecodingContext(value, cursor)) match {
              case ErrorResult(e) => errors += e
              case ValueResult(e) => successValues += e
            }
        }
        Diagnostic.fromDiagnostics(errors.result()) match {
          case Some(x) => ErrorResult(x)
          case None => ValueResult(successValues.result())
        }
      case _ =>
        ErrorResult(new TypeMismatchDiagnostic("JsonArray", context))
    }
  }

  implicit def objectJsonDecoder[A](implicit
      ev: JsonDecoder[A]
  ): JsonDecoder[Map[String, A]] = { context =>
    context.json match {
      case JsonObject(members) =>
        val successValues = Map.newBuilder[String, A]
        val errors = mutable.ListBuffer.empty[Diagnostic]
        members.foreach { member =>
          val cursor =
            SelectMemberCursor(member.key.value).withParent(context.cursor)
          ev.decode(DecodingContext(member.value, cursor)) match {
            case ErrorResult(error) => errors += error
            case ValueResult(value) =>
              successValues += (member.key.value -> value)
          }
        }
        Diagnostic.fromDiagnostics(errors.result()) match {
          case Some(x) => ErrorResult(x)
          case None => ValueResult(successValues.result())
        }
      case _ =>
        ErrorResult(new TypeMismatchDiagnostic("JsonObject", context))
    }
  }

  implicit def optionJsonDecoder[A](implicit
      ev: JsonDecoder[A]
  ): JsonDecoder[Option[A]] = { context =>
    context.json match {
      case JsonNull() => ValueResult(None)
      case other => ev.decode(context).map(Some(_))
    }
  }

}

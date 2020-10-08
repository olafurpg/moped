package moped.json

import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.compat._
import scala.collection.mutable

import moped.cli.Application
import moped.internal.diagnostics.TypeMismatchDiagnostic
import moped.reporters.Diagnostic

trait JsonDecoder[A] { self =>

  def decode(context: DecodingContext): DecodingResult[A]

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
      // TODO: missing context on success
      context.json,
      _ => ErrorResult(new TypeMismatchDiagnostic(expected, context))
    )
  }
  implicit val jsonElementJsonDecoder: JsonDecoder[JsonElement] =
    context => ValueResult(context.json)
  implicit val jsonStringDecoder: JsonDecoder[JsonString] =
    fromJson[JsonString]("String") {
      case j: JsonString => ValueResult(j)
    }
  implicit val intJsonDecoder: JsonDecoder[Int] =
    fromJson[Int]("Int") {
      case JsonNumber(value) => ValueResult(value.toInt)
    }
  implicit val doubleJsonDecoder: JsonDecoder[Double] =
    fromJson[Double]("Double") {
      case JsonNumber(value) => ValueResult(value)
    }
  implicit val stringJsonDecoder: JsonDecoder[String] =
    fromJson[String]("String") {
      case JsonString(value) => ValueResult(value)
    }
  implicit val booleanJsonDecoder: JsonDecoder[Boolean] =
    fromJson[Boolean]("Boolean") {
      case JsonBoolean(value) => ValueResult(value)
    }
  implicit val unitJsonDecoder: JsonDecoder[Unit] =
    constant(())
  implicit lazy val pathJsonDecoder: JsonDecoder[Path] =
    stringJsonDecoder.flatMap(path =>
      DecodingResult.fromUnsafe(() => Paths.get(path))
    )
  implicit val applicationJsonDecoder: JsonDecoder[Application] =
    context => ValueResult(context.app)

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
            ev.decode(context.withJson(value).withCursor(cursor)) match {
              case ErrorResult(e) => errors += e
              case ValueResult(e) => successValues += e
            }
        }
        Diagnostic.fromDiagnostics(errors.result()) match {
          case Some(x) => ErrorResult(x)
          case None => ValueResult(successValues.result())
        }
      case _ =>
        ErrorResult(new TypeMismatchDiagnostic("Array", context))
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
          ev.decode(context.withJson(member.value).withCursor(cursor)) match {
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
        ErrorResult(new TypeMismatchDiagnostic("Object", context))
    }
  }

  implicit def optionJsonDecoder[A](implicit
      ev: JsonDecoder[A]
  ): JsonDecoder[Option[A]] = { context =>
    context.json match {
      case JsonNull() => ValueResult(None)
      case _ => ev.decode(context).map(Some(_))
    }
  }

}

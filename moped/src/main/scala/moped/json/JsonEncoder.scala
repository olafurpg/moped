package moped.json

import java.nio.file.Path

import moped.cli.Application

trait JsonEncoder[A] {
  def encode(value: A): JsonElement

  final def contramap[B](f: B => A): JsonEncoder[B] =
    value => encode(f(value))
}

object JsonEncoder {
  def empty[A]: JsonEncoder[A] =
    _ => JsonNull()
  def apply[A](implicit ev: JsonEncoder[A]): JsonEncoder[A] =
    ev

  private val anyElementJsonEncoder: JsonEncoder[JsonElement] =
    elem => elem
  implicit def jsonElementEncoder[A <: JsonElement]: JsonEncoder[A] =
    anyElementJsonEncoder.asInstanceOf[JsonEncoder[A]]

  implicit val booleanJsonEncoder: JsonEncoder[Boolean] =
    value => JsonBoolean(value)
  implicit val stringJsonEncoder: JsonEncoder[String] =
    value => JsonString(value)
  implicit val intJsonEncoder: JsonEncoder[Int] =
    value => JsonNumber(value.toDouble)
  implicit val doubleJsonEncoder: JsonEncoder[Double] =
    value => JsonNumber(value)
  implicit val unitJsonEncoder: JsonEncoder[Unit] =
    _ => JsonNull()
  implicit val pathJsonEncoder: JsonEncoder[Path] =
    value => JsonString(value.toString())
  implicit val applicationJsonEncoder: JsonEncoder[Application] =
    value => JsonString(value.binaryName)

  implicit def iterableJsonEncoder[A, C[x] <: Iterable[x]](implicit
      ev: JsonEncoder[A]
  ): JsonEncoder[C[A]] =
    value => JsonArray(value.iterator.map(ev.encode).toList)

  implicit def mapJsonEncoder[A](implicit
      ev: JsonEncoder[A]
  ): JsonEncoder[Map[String, A]] =
    value => {
      JsonObject(value.iterator.map {
        case (key, value) =>
          JsonMember(JsonString(key), ev.encode(value))
      }.toList)
    }

  implicit def optionJsonEncoder[A](implicit
      ev: JsonEncoder[A]
  ): JsonEncoder[Option[A]] = {
    case Some(value) => ev.encode(value)
    case None => JsonNull()
  }

}

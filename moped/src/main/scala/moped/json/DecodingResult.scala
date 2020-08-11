package moped.json

import scala.util.control.NonFatal

import moped.internal.diagnostics.WithFilterDiagnostic
import moped.reporters.Diagnostic
import scala.collection.mutable

final case class ValueResult[+A](value: A) extends DecodingResult[A]
final case class ErrorResult(error: Diagnostic) extends DecodingResult[Nothing]

object DecodingResult {
  def value[A](a: A): DecodingResult[A] = ValueResult(a)
  def error[A](d: Diagnostic): DecodingResult[A] = ErrorResult(d)
  def fromResults[A](
      results: Iterable[DecodingResult[A]]
  ): DecodingResult[List[A]] = {
    val buf = mutable.ListBuffer.empty[A]
    val errors = mutable.ListBuffer.empty[Diagnostic]
    results.iterator.foreach {
      case ErrorResult(error) =>
        errors += error
      case ValueResult(value) =>
        buf += value
    }
    Diagnostic.fromDiagnostics(errors.toList) match {
      case Some(diagnostic) => ErrorResult(diagnostic)
      case None => ValueResult(buf.toList)
    }
  }
  def fromUnsafe[A](thunk: () => A): DecodingResult[A] =
    try ValueResult(thunk())
    catch { case NonFatal(e) => ErrorResult(Diagnostic.exception(e)) }
}

sealed abstract class DecodingResult[+A] extends Product with Serializable {

  def get: A =
    fold(identity, throw new NoSuchElementException())
  def getOrElse[B >: A](other: => B): B =
    fold(identity, _ => other)
  def orElse[B >: A](other: => DecodingResult[B]): DecodingResult[B] =
    fold(ValueResult(_), _ => other)

  def toOption: Option[A] =
    fold(Some(_), _ => None)
  def toEither: Either[Diagnostic, A] =
    fold(Right(_), Left(_))
  def toList: List[A] =
    fold(List(_), _ => List())
  def iterator(): Iterator[A] =
    fold(Iterator(_), _ => Iterator())

  def foreach[B](fn: A => B): Unit =
    fold(a => fn(a), _ => ())
  def map[B](fn: A => B): DecodingResult[B] =
    fold(a => ValueResult(fn(a)), ErrorResult(_))
  def flatMap[B](fn: A => DecodingResult[B]): DecodingResult[B] =
    fold(a => fn(a), ErrorResult(_))

  def filter(fn: A => Boolean): DecodingResult[A] =
    withFilter(fn)
  def withFilter(fn: A => Boolean): DecodingResult[A] =
    fold(
      a => {
        if (fn(a)) ValueResult(a)
        else ErrorResult(new WithFilterDiagnostic(a, fn))
      },
      ErrorResult(_)
    )

  /**
   * Use this method to upcast this type from `ErrorResult` or `ValueResult` into `DecodingResult[A]`
   *
   * @return this instance unchanged, this method is only used to influence type checking.
   */
  def upcast: DecodingResult[A] = this

  def fold[B](onValue: A => B, onError: Diagnostic => B): B =
    this match {
      case ValueResult(value) => onValue(value)
      case ErrorResult(error) => onError(error)
    }

  def product[B](other: DecodingResult[B]): DecodingResult[(A, B)] =
    (this, other) match {
      case (ErrorResult(a), ErrorResult(b)) => ErrorResult(a.mergeWith(b))
      case (ErrorResult(a), _) => ErrorResult(a)
      case (_, ErrorResult(b)) => ErrorResult(b)
      case (ValueResult(a), ValueResult(b)) => ValueResult((a, b))
    }

}

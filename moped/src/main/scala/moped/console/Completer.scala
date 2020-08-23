package moped.cli

import java.nio.file.Path

import moped.internal.console.PathCompleter

trait Completer[A] {
  def complete(context: TabCompletionContext): List[TabCompletionItem]
}

object Completer {
  def empty[T]: Completer[T] = _ => Nil
  implicit val pathCompleter: Completer[Path] = PathCompleter

  implicit def iterableCompleter[A, C[x] <: Iterable[x]](implicit
      ev: Completer[A]
  ): Completer[C[A]] = ev.asInstanceOf[Completer[C[A]]]
  implicit def optionCompleter[A](implicit
      ev: Completer[A]
  ): Completer[Option[A]] = ev.asInstanceOf[Completer[Option[A]]]

}

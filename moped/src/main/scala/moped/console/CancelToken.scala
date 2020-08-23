package moped.cli

import scala.concurrent.Promise
import scala.util.Success

final class CancelToken private (val onCanceled: Promise[Boolean]) {
  override def toString(): String = s"CancelToken(isCanceled=$isCanceled)"
  final def isCanceled: Boolean =
    onCanceled.isCompleted &&
      onCanceled.future.value == Some(Success(true))
  final def cancel(): Boolean =
    onCanceled.trySuccess(true)
}

object CancelToken {
  def empty(): CancelToken = new CancelToken(Promise())
}

package moped.internal.console

import scala.concurrent.ExecutionContext

object EagerExecutionContext extends ExecutionContext {
  def execute(runnable: Runnable): Unit = runnable.run()
  def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
}

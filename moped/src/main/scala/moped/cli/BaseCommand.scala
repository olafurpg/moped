package moped.cli

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

abstract class BaseCommand {
  def runAsFuture(): Future[Int]
}

abstract class Command extends BaseCommand {
  final override def runAsFuture(): Future[Int] =
    Try(run()) match {
      case Failure(exception) => Future.failed(exception)
      case Success(value) => Future.successful(value)
    }
  def run(): Int
}

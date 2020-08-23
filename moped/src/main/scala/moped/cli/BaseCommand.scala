package moped.cli

import scala.concurrent.Future
import scala.util.Try

abstract class BaseCommand {
  def runAsFuture(): Future[Int]
}

abstract class Command extends BaseCommand {
  final override def runAsFuture(): Future[Int] =
    Future.fromTry(Try(run()))
  def run(): Int
}

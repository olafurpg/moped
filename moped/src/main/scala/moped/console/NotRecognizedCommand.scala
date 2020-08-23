package moped.cli
import scala.collection.immutable.Nil

import moped.internal.reporters.Levenshtein

object NotRecognizedCommand extends Command {

  def run(app: Application): Int = {
    app.arguments match {
      case Nil =>
      case head :: _ =>
        notRecognized(head, app)
    }
    1
  }

  def notRecognized(subcommand: String, app: Application): Unit = {
    val closestSubcommand = Levenshtein.closestCandidate(
      subcommand,
      app.commands.map(_.subcommandName)
    )
    val didYouMean = closestSubcommand match {
      case None => ""
      case Some(candidate) =>
        s"\n\tDid you mean '${app.binaryName} $candidate'?"
    }
    app.error(
      s"no such subcommand '$subcommand'.$didYouMean\n\tTry '${app.binaryName} help' for more information."
    )
  }

}

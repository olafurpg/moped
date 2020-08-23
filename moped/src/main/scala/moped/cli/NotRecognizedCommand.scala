package moped.cli
import scala.collection.immutable.Nil

import moped.internal.reporters.Levenshtein

class NotRecognizedCommand(app: Application) extends Command {

  def run(): Int = {
    app.arguments match {
      case Nil =>
      case head :: _ =>
        notRecognized(head)
    }
    1
  }

  def notRecognized(subcommand: String): Unit = {
    val closestSubcommand = Levenshtein.closestCandidate(
      subcommand,
      app.commands.filterNot(_.isHidden).map(_.subcommandName)
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

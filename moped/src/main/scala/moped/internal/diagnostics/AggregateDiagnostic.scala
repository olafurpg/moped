package moped.internal.diagnostics

import scala.collection.immutable.Nil

import moped.reporters.Diagnostic
import moped.reporters.ErrorSeverity
import moped.reporters.NoPosition

class AggregateDiagnostic(head: Diagnostic, tail: List[Diagnostic])
    extends Diagnostic(ErrorSeverity, "", NoPosition, None, head :: tail) {
  def message: String =
    causes match {
      case Nil => "AggregateDiagnostic(Nil)"
      case head :: Nil => head.message
      case _ =>
        val count = causes.size
        val summary = if (count > 1) s"$count errors\n" else ""
        causes.zipWithIndex
          .map {
            case (d, i) =>
              s"[E$i] ${d.message}"
          }
          .mkString(summary, "\n", "")
    }
}

package moped.reporters

import scala.collection.mutable
import moped.internal.diagnostics._
import moped.json.DecodingContext

abstract class Diagnostic(
    val severity: Severity,
    val id: String = "",
    val position: Position = NoPosition,
    val throwable: Option[Throwable] = None,
    val causes: List[Diagnostic] = Nil
) {
  private val severityOverrides = mutable.ListBuffer.empty[Diagnostic]

  def mergeWith(other: Diagnostic): Diagnostic = {
    Diagnostic.fromDiagnostics(this, other :: Nil)
  }

  def message: String

  def all: List[Diagnostic] = {
    val buf = mutable.ListBuffer.empty[Diagnostic]
    def loop(d: Diagnostic): Unit = {
      buf += d
      d.causes.foreach(loop)
    }
    loop(this)
    buf.result()
  }

  def overrideSeverity(why: String, newSeverity: Severity): Unit = {
    severityOverrides += new OverrideSeverityDiagnostic(why, newSeverity)
  }
}

object Diagnostic {
  def message(value: String): Diagnostic = new MessageOnlyDiagnostic(value)
  def typeMismatch(expected: String, context: DecodingContext): Diagnostic =
    new TypeMismatchDiagnostic(expected, context)
  def fromDiagnostics(head: Diagnostic, other: List[Diagnostic]): Diagnostic = {
    fromDiagnostics(head :: other).getOrElse(head)
  }
  def fromDiagnostics(diagnostics: List[Diagnostic]): Option[Diagnostic] = {
    val flatDiagnostics = diagnostics.flatMap {
      case a: AggregateDiagnostic => a.causes
      case d => List(d)
    }
    flatDiagnostics match {
      case Nil => None
      case head :: Nil => Some(head)
      case head :: tail => Some(new AggregateDiagnostic(head, tail))
    }
  }
}

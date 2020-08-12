package moped.internal.diagnostics

import moped.reporters.Severity
import moped.reporters.NoPosition
import moped.reporters.Position

class MessageOnlyDiagnostic(
    val message: String,
    severity: Severity,
    pos: Position = NoPosition,
    throwable: Option[Throwable] = None
) extends moped.reporters.Diagnostic(
      severity,
      position = pos,
      throwable = throwable
    )

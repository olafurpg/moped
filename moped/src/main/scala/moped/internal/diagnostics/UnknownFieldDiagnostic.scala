package moped.internal.diagnostics

import moped.json.JsonString
import moped.reporters.Diagnostic
import moped.reporters.ErrorSeverity

class UnknownFieldDiagnostic(fieldName: JsonString)
    extends Diagnostic(
      ErrorSeverity,
      "",
      fieldName.position
    ) {
  def message: String = s"unknown field name '${fieldName.value}'"
}

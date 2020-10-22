package moped.internal.diagnostics

import moped.reporters.Diagnostic
import moped.reporters.ErrorSeverity
import moped.internal.console.CommandLineParser
import moped.json.JsonMember

class UnknownFieldDiagnostic(member: JsonMember)
    extends Diagnostic(
      ErrorSeverity,
      "",
      member.key.position
    ) {
  def fieldName = member.key.value
  def value = member.value.toDoc.render(10000)
  def message: String = {
    if (fieldName == CommandLineParser.PositionalArgument) {
      s"unexpected positional arguments ${value}"
    } else if (fieldName == CommandLineParser.TrailingArgument) {
      s"unexpected trailing arguments ${value}"
    } else {
      s"unknown field name '${fieldName}' with value ${value}"
    }
  }
}

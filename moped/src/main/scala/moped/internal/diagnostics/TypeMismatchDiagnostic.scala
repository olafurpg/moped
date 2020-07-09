package moped.internal.diagnostics

import moped.Position
import moped.Diagnostic
import moped.ErrorSeverity
import moped.Cursor
import moped.JsonElement
import moped.DecodingContext

class TypeMismatchDiagnostic(
    expected: String,
    obtained: String,
    pos: Position,
    cursor: Cursor
) extends Diagnostic(ErrorSeverity, "TypeMismatch", pos) {
  def this(expected: String, context: DecodingContext) =
    this(
      expected,
      context.json.productPrefix,
      context.json.position,
      context.cursor
    )
  override def message: String = {
    val pathSuffix = if (cursor.isEmpty) "" else s"at ${cursor.path}"
    s"""Type mismatch$pathSuffix;
       |  found    : $obtained
       |  expected : $expected""".stripMargin
  }
}
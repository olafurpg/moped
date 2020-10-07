package moped.internal.diagnostics

import moped.json._
import moped.reporters._

class TypeMismatchDiagnostic(
    expected: String,
    obtained: String,
    pos: Position,
    cursor: Cursor
) extends Diagnostic(ErrorSeverity, "TypeMismatch", pos) {
  def this(expected: String, obtained: String, context: DecodingContext) =
    this(
      expected,
      obtained,
      context.json.position,
      context.cursor
    )
  def this(expected: String, context: DecodingContext) =
    this(
      expected,
      context.json.productPrefix.stripPrefix("Json"),
      context
    )
  override def message: String = {
    val pathSuffix = if (cursor.isEmpty) "" else s" at '${cursor.path}'"
    s"""Type mismatch$pathSuffix;
       |  found    : $obtained
       |  expected : $expected""".stripMargin
  }
}

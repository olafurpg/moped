package moped.parsers

import moped.json.{DecodingResult, JsonElement}
import moped.reporters.Input
import org.dhallj.syntax._
import org.dhallj.parser.support.JavaCCParserInternals
import moped.internal.transformers.DhallTransformer
import scala.util.control.NonFatal
import org.dhallj.parser.support.ParseException
import org.dhallj.parser.support.JavaCCParserException
import moped.reporters.RangePosition
import moped.internal.diagnostics.DiagnosticException
import moped.reporters.Diagnostic

object DhallParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("dhall")
  def parse(input: Input): DecodingResult[JsonElement] =
    DecodingResult.fromUnsafe { () =>
      try {
        JavaCCParserInternals.parse(input.text)
      } catch {
        case e: JavaCCParserException =>
          val start = input.lineToOffset(e.startLine - 1) + e.startColumn
          val end = input.lineToOffset(e.endLine - 1) + e.endColumn
          val pos = RangePosition(input, start, end)
          throw new DiagnosticException(Diagnostic.error(e.getMessage(), pos))
      }
      input.text.parseExpr match {
        case Left(value) => throw value
        case Right(value) =>
          DhallTransformer.transform(value.normalize(), JsonElement)
      }
    }
}

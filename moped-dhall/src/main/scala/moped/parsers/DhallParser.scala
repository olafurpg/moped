package moped.parsers

import moped.internal.diagnostics.DiagnosticException
import moped.internal.transformers.DhallTransformer
import moped.internal.transformers.JsonTransformer
import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Diagnostic
import moped.reporters.Input
import moped.reporters.RangePosition
import org.dhallj.parser.support.JavaCCParserException
import org.dhallj.parser.support.JavaCCParserInternals

object DhallParser extends DhallParser
class DhallParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("dhall")
  def parse(input: Input): DecodingResult[JsonElement] =
    DecodingResult.fromUnsafe { () =>
      try {
        val value = JavaCCParserInternals.parse(input.text)
        new DhallTransformer(input)
          .transform(value.normalize(), new JsonTransformer(input))
      } catch {
        case e: JavaCCParserException =>
          val start = input.lineToOffset(e.startLine - 1) + e.startColumn
          val end = input.lineToOffset(e.endLine - 1) + e.endColumn
          val pos = RangePosition(input, start, end)
          throw new DiagnosticException(Diagnostic.error(e.getMessage(), pos))
      }
    }
}

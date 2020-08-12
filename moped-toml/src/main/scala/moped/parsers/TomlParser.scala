package moped.parsers

import scala.meta.internal.fastparse.all.End
import scala.meta.internal.fastparse.core.Parsed.Failure
import scala.meta.internal.fastparse.core.Parsed.Success

import moped.internal.diagnostics.DiagnosticException
import moped.internal.transformers.TomlTransformer
import moped.json.Cursor
import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Diagnostic
import moped.reporters.Input
import moped.reporters.RangePosition
import toml.Embed
import toml.Rules

object TomlParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("toml")
  def parse(input: Input): DecodingResult[JsonElement] = {
    DecodingResult.fromUnsafe { () =>
      val rules =
        new Rules(Set(toml.Extension.MultiLineInlineTables))
      val parsed = rules.root.parse(input.text)
      parsed match {
        case f @ Failure(lastParser, index, extra) =>
          val pos = RangePosition(input, index, index).endOfFileOffset
          val message = lastParser match {
            case End => "incomplete TOML"
            case _ => f.msg
          }
          throw new DiagnosticException(
            Diagnostic.error(message, pos)
          )
        case Success(value, _) =>
          Embed.root(value) match {
            case Left((address, message)) =>
              val cursor = Cursor.fromPath(address)
              throw new DiagnosticException(Diagnostic.error(message))
            case Right(value) =>
              TomlTransformer.transform(value, JsonElement)
          }
      }
    }
  }
}

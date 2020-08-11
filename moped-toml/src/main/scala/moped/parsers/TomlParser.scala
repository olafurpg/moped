package moped.parsers

import moped.json.{DecodingResult, JsonElement}
import moped.reporters.Input
import moped.internal.transformers.TomlTransformer
import moped.reporters.Diagnostic
import moped.internal.diagnostics.DiagnosticException
import moped.json.Cursor

object TomlParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("toml")
  def parse(input: Input): DecodingResult[JsonElement] = {
    DecodingResult.fromUnsafe { () =>
      val parsed =
        toml.Toml.parse(input.text, Set(toml.Extension.MultiLineInlineTables))
      parsed match {
        case Left((address, message)) =>
          val cursor = Cursor.fromPath(address)
          throw new DiagnosticException(Diagnostic.error(message))
        case Right(value) =>
          TomlTransformer.transform(value, JsonElement)
      }
    }
  }
}

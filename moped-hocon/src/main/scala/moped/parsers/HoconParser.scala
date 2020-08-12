package moped.parsers

import moped.json.{DecodingResult, JsonElement}
import moped.reporters.Input
import org.ekrich.config.ConfigFactory
import moped.internal.transformers.HoconTransformer
import org.ekrich.config.ConfigParseOptions
import scala.util.control.NonFatal
import org.ekrich.config.ConfigException
import moped.reporters.NoPosition
import moped.reporters.RangePosition
import moped.internal.diagnostics.DiagnosticException
import moped.reporters.Diagnostic

object HoconParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("conf")
  def parse(input: Input): DecodingResult[JsonElement] =
    DecodingResult.fromUnsafe { () =>
      try {
        val options =
          ConfigParseOptions.defaults.setOriginDescription(input.filename)
        val root = ConfigFactory.parseString(input.text, options).resolve().root
        HoconTransformer.transform(root, JsonElement)
      } catch {
        case e: ConfigException.Parse
            if e.getMessage != null &&
              e.origin != null =>
          val pos =
            if (e.origin.lineNumber < 0) {
              NoPosition
            } else {
              val line = e.origin.lineNumber - 1
              val offset = input.lineToOffset(line)
              RangePosition(input, offset, offset)
            }
          val message = e
            .getMessage()
            .stripPrefix(s"${input.filename}: ${e.origin.lineNumber}: ")
          throw new DiagnosticException(Diagnostic.error(message, pos))
        case NonFatal(e) =>
          throw e
      }
    }
}

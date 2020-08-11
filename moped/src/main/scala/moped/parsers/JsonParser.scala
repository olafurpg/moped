package moped.parsers

import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Input
import ujson._
import scala.util.control.NonFatal
import moped.json.ValueResult
import moped.json.ErrorResult
import moped.reporters.Diagnostic
import moped.internal.transformers.InputTransformer

object JsonParser extends ConfigurationParser {
  override val supportedFileExtensions: List[String] = List("json")
  override def parse(input: Input): DecodingResult[JsonElement] = {
    try {
      val readable = Readable.fromTransformer(input, InputTransformer)
      ValueResult(readable.transform(JsonElement))
    } catch {
      case NonFatal(e) =>
        ErrorResult(Diagnostic.exception(e))
    }
  }
}

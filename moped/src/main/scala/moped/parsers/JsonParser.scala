package moped.parsers

import moped.internal.transformers.InputTransformer
import moped.internal.transformers.JsonTransformer
import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Input
import ujson._

object JsonParser extends JsonParser
class JsonParser extends ConfigurationParser {
  override val supportedFileExtensions: List[String] = List("json")
  override def parse(input: Input): DecodingResult[JsonElement] = {
    DecodingResult.fromUnsafe[JsonElement] { () =>
      val readable = Readable.fromTransformer(input, InputTransformer)
      readable.transform(JsonTransformer)
    }
  }
}

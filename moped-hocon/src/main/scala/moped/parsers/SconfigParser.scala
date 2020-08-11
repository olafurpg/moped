package moped.parsers

import moped.json.{DecodingResult, JsonElement}
import moped.reporters.Input
import org.ekrich.config.ConfigFactory
import moped.internal.transformers.SconfigTransformer

object SconfigParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("conf")
  def parse(input: Input): DecodingResult[JsonElement] =
    DecodingResult.fromUnsafe { () =>
      val root = ConfigFactory.parseString(input.text).resolve().root
      SconfigTransformer.transform(root, JsonElement)
    }
}

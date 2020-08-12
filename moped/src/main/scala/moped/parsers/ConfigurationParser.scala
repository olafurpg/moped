package moped.parsers

import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Input

trait ConfigurationParser {
  def supportedFileExtensions: List[String]
  def parse(input: Input): DecodingResult[JsonElement]
}

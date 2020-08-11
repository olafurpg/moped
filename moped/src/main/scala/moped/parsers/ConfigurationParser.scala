package moped.parsers

import moped.reporters.Input
import moped.json.DecodingResult
import moped.json.JsonElement

trait ConfigurationParser {
  def supportedFileExtensions: List[String]
  def parse(input: Input): DecodingResult[JsonElement]
}

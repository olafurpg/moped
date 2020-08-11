package moped.parsers

import moped.json.{DecodingResult, JsonElement}
import moped.reporters.Input
import org.dhallj.syntax._
import moped.internal.transformers.DhallTransformer

object DhallParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("dhall")
  def parse(input: Input): DecodingResult[JsonElement] =
    DecodingResult.fromUnsafe { () =>
      input.text.parseExpr match {
        case Left(value) => throw value
        case Right(value) =>
          DhallTransformer.transform(value.normalize(), JsonElement)
      }
    }
}

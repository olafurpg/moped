package moped.parsers

import moped.json.{DecodingResult, JsonElement}
import moped.reporters.Input
import org.yaml.snakeyaml.Yaml
import moped.internal.transformers.YamlTransformer
import moped.internal.transformers.YamlElement

object YamlParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("yml", "yaml")
  def parse(input: Input): DecodingResult[JsonElement] = {
    DecodingResult.fromUnsafe { () =>
      val yaml = new Yaml().load[Object](input.text)
      YamlTransformer.transform(new YamlElement(yaml), JsonElement)
    }
  }
}

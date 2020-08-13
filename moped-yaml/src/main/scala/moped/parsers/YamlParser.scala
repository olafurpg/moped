package moped.parsers

import moped.internal.diagnostics.DiagnosticException
import moped.internal.transformers.JsonTransformer
import moped.internal.transformers.YamlElement
import moped.internal.transformers.YamlTransformer
import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Diagnostic
import moped.reporters.Input
import moped.reporters.RangePosition
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.MarkedYAMLException

object YamlParser extends YamlParser
class YamlParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("yml", "yaml")
  def parse(input: Input): DecodingResult[JsonElement] = {
    DecodingResult.fromUnsafe { () =>
      try {
        val yaml = new Yaml().load[Object](input.text)
        YamlTransformer.transform(new YamlElement(yaml), JsonTransformer)
      } catch {
        case e: MarkedYAMLException
            if e.getProblem != null &&
              e.getProblemMark != null &&
              e.getProblemMark().getIndex() >= 0 =>
          val offset = e.getProblemMark().getIndex()
          val pos = RangePosition(input, offset, offset)
          throw new DiagnosticException(Diagnostic.error(e.getProblem(), pos))
      }
    }
  }
}

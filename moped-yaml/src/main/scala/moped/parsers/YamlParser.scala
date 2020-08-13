package moped.parsers

import moped.internal.diagnostics.DiagnosticException
import moped.internal.transformers.JsonTransformer
import moped.internal.transformers.YamlNodeTransformer
import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Diagnostic
import moped.reporters.Input
import moped.reporters.RangePosition
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.error.MarkedYAMLException
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import org.yaml.snakeyaml.resolver.Resolver

object YamlParser extends YamlParser
class YamlParser extends ConfigurationParser {
  def supportedFileExtensions: List[String] = List("yml", "yaml")
  def parse(input: Input): DecodingResult[JsonElement] = {
    DecodingResult.fromUnsafe { () =>
      try {
        val composer = new Composer(
          new ParserImpl(new StreamReader(input.text)),
          new Resolver(),
          new LoaderOptions()
        )
        val node = composer.getSingleNode()
        new YamlNodeTransformer(input)
          .transform(node, new JsonTransformer(input))
      } catch {
        case e: MarkedYAMLException
            if e.getProblem != null &&
              e.getProblemMark != null &&
              e.getProblemMark().getIndex() >= 0 =>
          val offset = e.getProblemMark().getIndex()
          val pos = RangePosition(input, offset, offset)
          throw new DiagnosticException(Diagnostic.error(e.getProblem(), pos))
        case e: Throwable =>
          e.printStackTrace()
          throw e
      }
    }
  }
}

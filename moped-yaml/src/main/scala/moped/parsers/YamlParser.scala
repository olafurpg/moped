package moped.parsers

import moped.internal.diagnostics.DiagnosticException
import moped.internal.transformers.YamlElement
import moped.internal.transformers.YamlTransformer
import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Diagnostic
import moped.reporters.Input
import moped.reporters.RangePosition
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.MarkedYAMLException
import moped.internal.transformers.JsonTransformer
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.LoaderOptions
import fansi.ErrorMode.Throw

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
        pprint.log(node)
        pprint.log(node.getClass())
        val yaml = new Yaml().load[Object](input.text)
        // val node = new Yaml().load[Node](input.text)
        // pprint.log(node)
        YamlTransformer.transform(new YamlElement(yaml), JsonTransformer)
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

package moped.parsers

import fastparse.Parsed.Failure
import moped.internal.diagnostics.DiagnosticException
import moped.internal.transformers.JsonTransformer
import moped.internal.transformers.JsonnetInterpreter
import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Diagnostic
import moped.reporters.Input
import moped.reporters.NoPosition
import moped.reporters.Position

object JsonnetParser extends JsonnetParser(JsonnetInterpreter())
class JsonnetParser(interpreter: JsonnetInterpreter)
    extends ConfigurationParser {

  def supportedFileExtensions: List[String] = List("jsonnet")
  def parse(input: Input): DecodingResult[JsonElement] = {
    DecodingResult.fromUnsafe { () =>
      val interpreted = interpreter.interp.interpret(
        input.text,
        sjsonnet.OsPath(
          os.Path(
            input.path.getOrElse(
              interpreter.workingDirectory.resolve(input.filename)
            )
          )
        )
      )
      interpreted match {
        case Left(error) =>
          val pos: Position =
            fastparse.parse(input.text, sjsonnet.Parser.document(_)) match {
              case f: Failure =>
                Position.offset(input, f.index)
              case _ =>
                NoPosition
            }
          throw new DiagnosticException(Diagnostic.error(error, pos))
        case Right(value) =>
          value.transform(new JsonTransformer(input))
      }
    }
  }

}

package moped.parsers

import moped.internal.diagnostics.DiagnosticException
import moped.internal.transformers.JsonTransformer
import moped.internal.transformers.JsonnetInterpreter
import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Diagnostic
import moped.reporters.Input

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
          throw new DiagnosticException(Diagnostic.error(error))
        case Right(value) =>
          value.transform(JsonTransformer)
      }
    }
  }

}

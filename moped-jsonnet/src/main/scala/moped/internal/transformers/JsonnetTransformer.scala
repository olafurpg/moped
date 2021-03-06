package moped.internal.transformers

import java.nio.file.Path

import moped.cli.Environment
import sjsonnet.Cli
import sjsonnet.Interpreter
import sjsonnet.SjsonnetMain

case class JsonnetInterpreter(
    workingDirectory: Path = Environment.default.workingDirectory,
    config: Cli.Config = Cli.Config()
) {
  val wd: os.Path = os.Path(workingDirectory)
  val interp = new Interpreter(
    collection.mutable.Map.empty,
    Map.empty,
    Map.empty,
    sjsonnet.OsPath(wd),
    importer = SjsonnetMain.resolveImport(
      config.jpaths.map(os.Path(_, wd)).map(sjsonnet.OsPath(_)),
      allowedInputs = None
    ),
    config.preserveOrder
  )
}

package moped

import sjsonnet.Interpreter
import sjsonnet.SjsonnetMain
import java.nio.file.Path
import sjsonnet.Cli

case class JsonnetElement(workingDirectory: Path, config: Cli.Config) {
  val wd = os.Path(workingDirectory)
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

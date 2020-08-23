package moped.internal.console

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import moped.cli.Completer
import moped.cli.TabCompletionContext
import moped.cli.TabCompletionItem

case object PathCompleter extends Completer[Path] {

  def complete(context: TabCompletionContext): List[TabCompletionItem] = {
    val pathOrDirectory = Paths.get(context.last)
    val absolutePathOrDirectory =
      if (pathOrDirectory.isAbsolute()) pathOrDirectory
      else context.app.env.workingDirectory.resolve(pathOrDirectory)
    val path: Path =
      if (context.last.endsWith(File.separator)) {
        absolutePathOrDirectory
      } else if (context.last.isEmpty()) {
        absolutePathOrDirectory
      } else {
        Option(absolutePathOrDirectory.getParent())
          .getOrElse(absolutePathOrDirectory)
      }
    if (Files.isDirectory(path)) {
      path
        .toFile()
        .listFiles()
        .iterator
        .map(_.toPath())
        .map { p =>
          val slash = if (Files.isDirectory(p)) File.separator else ""
          val prefix =
            if (pathOrDirectory.isAbsolute()) {
              p.toString()
            } else {
              context.app.env.workingDirectory.relativize(p).toString()
            }
          prefix + slash
        }
        .map(TabCompletionItem(_))
        .toList
        .sortBy(_.name)
    } else {
      Nil
    }
  }

}

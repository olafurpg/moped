package moped.console

import moped.reporters.Input
import java.nio.file.Path
import scala.collection.mutable
import java.nio.file.Files

trait ConfigurationSearcher {
  def find(app: Application): List[Input]
}

object EmptyConfigurationSearcher extends ConfigurationSearcher {
  def find(app: Application): List[Input] = List()
}

/**
 * Searcher that's inspired by cosmiconf (https://github.com/davidtheclark/cosmiconfig)
 */
object ParentsConfigurationSearcher extends ConfigurationSearcher {
  def find(app: Application): List[Input] = {
    val buf = mutable.ListBuffer.empty[Input]
    val startDir = app.env.workingDirectory
    val stopDir = app.env.homeDirectory
    val extensions = app.parsers.flatMap(_.supportedFileExtensions)
    def visitFile(file: Path): Unit = {
      if (Files.isRegularFile(file) && Files.isReadable(file)) {
        buf += Input.path(file)
      }
    }
    def visitExtensions(dir: Path, filename: String): Unit = {
      extensions.foreach { extension =>
        visitFile(dir.resolve(s"$filename.$extension"))
      }
    }
    def visitDirectory(dir: Path): Unit = {
      if (Files.isDirectory(dir)) {
        visitExtensions(dir.resolve(".config"), app.binaryName)
        visitExtensions(dir, "." + app.binaryName)
        Option(dir.getParent()).foreach { parent =>
          if (parent.startsWith(stopDir)) {
            visitDirectory(parent)
          }
        }
      }
    }
    visitDirectory(startDir)
    visitDirectory(app.env.preferencesDirectory)
    buf.toList
  }
}

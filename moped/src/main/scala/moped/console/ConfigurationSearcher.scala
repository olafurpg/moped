package moped.console

import java.nio.file.Files
import java.nio.file.Path

import scala.collection.mutable

import moped.json.DecodingResult
import moped.json.JsonElement
import moped.reporters.Input

trait ConfigurationSearcher {
  def find(app: Application): List[DecodingResult[JsonElement]]
}
object ConfigurationSearcher {
  def candidates(
      app: Application,
      directory: Path,
      filename: String
  ): List[DecodingResult[JsonElement]] = {
    for {
      parser <- app.parsers
      extension <- parser.supportedFileExtensions
      file = directory.resolve(s"$filename.$extension")
      if Files.isRegularFile(file) && Files.isReadable(file)
    } yield parser.parse(Input.path(file))
  }
}

object EmptySearcher extends ConfigurationSearcher {
  def find(app: Application): List[DecodingResult[JsonElement]] = List()
}

object SystemSearcher extends ConfigurationSearcher {
  def find(app: Application): List[DecodingResult[JsonElement]] = {
    ConfigurationSearcher.candidates(
      app,
      app.env.preferencesDirectory,
      app.binaryName
    )
  }
}

object ProjectSearcher extends ConfigurationSearcher {
  def find(app: Application): List[DecodingResult[JsonElement]] = {
    val buf = mutable.ListBuffer.empty[DecodingResult[JsonElement]]
    val cwd = app.env.workingDirectory
    List(
      ConfigurationSearcher.candidates(
        app,
        cwd.resolve(".config"),
        app.binaryName
      ),
      ConfigurationSearcher.candidates(
        app,
        cwd,
        "." + app.binaryName
      )
    ).flatten
  }
}

class AggregateSearcher(val underlying: List[ConfigurationSearcher])
    extends ConfigurationSearcher {
  def find(app: Application): List[DecodingResult[JsonElement]] =
    underlying.flatMap(_.find(app))
}

package moped.testkit

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes

import scala.collection.mutable

import moped.internal.console.Utils

object FileLayout {
  def asString(
      root: Path,
      includePath: Path => Boolean = _ => true,
      charset: Charset = StandardCharsets.UTF_8
  ): String = {
    if (!Files.isDirectory(root)) return ""
    import scala.collection.JavaConverters._
    val out = new StringBuilder()
    val buf = mutable.ArrayBuffer.empty[Path]
    Files.walkFileTree(
      root,
      new SimpleFileVisitor[Path] {
        override def preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          if (!includePath(dir)) FileVisitResult.SKIP_SUBTREE
          else FileVisitResult.CONTINUE
        }
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          if (includePath(file)) {
            buf += file
          }
          FileVisitResult.CONTINUE
        }
      }
    )
    buf.sorted.foreach { file =>
      val relpath = root.relativize(file).iterator().asScala.mkString("/")
      out.append("/").append(relpath)
      if (Files.isSymbolicLink(file)) {
        out
          .append(" -> ")
          .append(Files.readSymbolicLink(file))

      } else {
        val text = Utils.readFile(file)
        out.append("\n").append(text)
      }
      out.append("\n")
    }
    out.toString()
  }

  def mapFromString(layout: String): Map[String, String] = {
    if (!layout.trim.isEmpty) {
      val lines = layout.replace("\r\n", "\n")
      lines
        .split("(?=\n/)")
        .map { row =>
          row.stripPrefix("\n").split("\n", 2).toList match {
            case path :: contents :: Nil =>
              val withEndOfFileLine =
                if (contents.endsWith("\n")) contents else contents + "\n"
              path.stripPrefix("/") -> withEndOfFileLine
            case els =>
              throw new IllegalArgumentException(
                s"Unable to split argument info path/contents! \n$els"
              )
          }
        }
        .toMap
    } else {
      Map.empty
    }
  }

  def fromString(
      layout: String,
      root: Path = Files.createTempDirectory("moped"),
      charset: Charset = StandardCharsets.UTF_8
  ): Path = {
    if (!layout.trim.isEmpty) {
      mapFromString(layout).foreach {
        case (path, contents) =>
          val file =
            path.split("/").foldLeft(root)(_ resolve _)
          val parent = file.getParent
          if (!Files.exists(parent)) { // cannot create directories when parent is a symlink
            Files.createDirectories(parent)
          }
          Files.deleteIfExists(file)
          Files.write(
            file,
            contents.getBytes(charset),
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE
          )
      }
    }
    root
  }
}

package moped.testkit

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class DeleteVisitor extends SimpleFileVisitor[Path] {
  override def visitFile(
      file: Path,
      attrs: BasicFileAttributes
  ): FileVisitResult = {
    Files.deleteIfExists(file)
    FileVisitResult.CONTINUE
  }
  override def postVisitDirectory(
      dir: Path,
      exc: IOException
  ): FileVisitResult = {
    Files.deleteIfExists(dir)
    FileVisitResult.CONTINUE
  }
}

object DeleteVisitor {
  def deleteRecursively(path: Path): Int = {
    try {
      Files.walkFileTree(path, new DeleteVisitor)
      0
    } catch {
      case e: NoSuchFileException =>
        0
      case e: IOException =>
        e.printStackTrace()
        1
    }
  }
}

package moped.reporters

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import scala.collection.mutable
import scala.util.hashing.MurmurHash3

object Input {
  val none: Input =
    new Input("<none>", "", None, Some(StandardCharsets.UTF_8))
  def string(text: String): Input =
    new Input("<string>", text, None, Some(StandardCharsets.UTF_8))
  def filename(filename: String, text: String): Input =
    new Input(filename, text, None, Some(StandardCharsets.UTF_8))
  def path(path: Path): Input =
    Input.path(path, StandardCharsets.UTF_8)
  def path(path: Path, charset: Charset): Input =
    new Input(
      path.toString(),
      // TODO(olafur): support a method that returns DecodingResult[Input]
      new String(Files.readAllBytes(path), charset),
      Some(path),
      Some(StandardCharsets.UTF_8)
    )
  def file(file: File): Input =
    Input.path(file.toPath(), StandardCharsets.UTF_8)
}

final class Input private (
    val filename: String,
    val text: String,
    val path: Option[Path],
    val charset: Option[Charset]
) {

  final val chars: Array[Char] = text.toCharArray

  private val cachedLineIndices: Array[Int] = {
    val buf = new mutable.ArrayBuffer[Int]
    buf += 0
    var i = 0
    while (i < chars.length) {
      if (chars(i) == '\n')
        buf += (i + 1)
      i += 1
    }
    if (buf.last != chars.length)
      buf += chars.length // sentinel value used for binary search
    buf.toArray
  }

  def isEmpty: Boolean = text.isEmpty()

  def lineCount: Int = cachedLineIndices.length

  def lineToOffset(line: Int): Int = {
    // NOTE: The length-1 part is not a typo, it's to accommodate the sentinel value.
    if (!(0 <= line && line <= cachedLineIndices.length - 1)) {
      val message =
        s"$line is not a valid line number, allowed [0..${cachedLineIndices.length - 1}]"
      throw new IllegalArgumentException(message)
    }
    cachedLineIndices(line)
  }

  def offsetToLine(offset: Int): Int = {
    val chars = this.chars
    val a = cachedLineIndices
    // NOTE: We allow chars.length, because it's a valid value for an offset.
    if (!(0 <= offset && offset <= chars.length)) {
      val message =
        s"$offset is not a valid offset, allowed [0..${chars.length}]"
      throw new IllegalArgumentException(message)
    }
    // If the file doesn't end with \n, then it's simply last_line:last_col+1.
    // But if the file does end with \n, then it's last_line+1:0.
    if (
      offset == chars.length && (0 < chars.length && chars(offset - 1) == '\n')
    ) {
      return a.length - 1
    }
    var lo = 0
    var hi = a.length - 1
    while (hi - lo > 1) {
      val mid = (hi + lo) / 2
      if (offset < a(mid)) hi = mid
      else if (a(mid) == offset) return mid
      else /* if (a(mid) < offset */ lo = mid
    }
    lo
  }

  override def toString(): String = {
    path match {
      case Some(value) =>
        s"Input.path($value)"
      case None =>
        val truncatedText = pprint.PPrinter.BlackWhite.tokenize(text).mkString
        s"Input(filename=$filename, text=$truncatedText)"
    }
  }

  override def hashCode(): Int = {
    var h = 0xabcdabcd
    h = MurmurHash3.mix(h, filename.##)
    h = MurmurHash3.mix(h, text.##)
    h = MurmurHash3.mix(h, path.##)
    h = MurmurHash3.mix(h, charset.##)
    MurmurHash3.finalizeHash(h, 4)
  }

  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || {
      obj match {
        case i: Input =>
          i.filename == this.filename &&
            i.text == this.text &&
            i.path == this.path &&
            i.charset == this.charset
        case _ => false
      }
    }

}

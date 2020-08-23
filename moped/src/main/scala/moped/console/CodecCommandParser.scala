package moped.cli

import moped.internal.console.HelpMessage
import moped.json.DecodingContext
import moped.json.DecodingResult
import moped.json.JsonCodec
import moped.json.JsonElement
import moped.macros.ClassShape
import moped.macros.ClassShaper
import org.typelevel.paiges.Doc

class CodecCommandParser[A <: BaseCommand](
    codec: JsonCodec[A],
    default: A,
    val shape: ClassShape
) extends CommandParser[A] {
  def this(codec: JsonCodec[A], default: A) = this(codec, default, codec.shape)
  override def options: Doc =
    HelpMessage.generate(default)(codec, ClassShaper(shape))
  def decode(context: DecodingContext): DecodingResult[A] =
    codec.decode(context)
  def encode(value: A): JsonElement =
    codec.encode(value)
  override def withApplication(app: Application): CommandParser[A] =
    new CodecCommandParser[A](codec, default, app.preProcessClassShape(shape)) {
      override def nestedCommands: List[CommandParser[_]] = {
        super.nestedCommands.map(_.withApplication(app))
      }
    }
}

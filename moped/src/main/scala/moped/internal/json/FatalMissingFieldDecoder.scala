package moped.internal.json

import moped.json.DecodingContext
import moped.json.DecodingResult
import moped.macros.ClassShaper

object FatalMissingFieldDecoder {
  def decode[T](
      context: DecodingContext,
      ev: ClassShaper[T]
  ): DecodingResult[Unit] = {
    ???
  }
}

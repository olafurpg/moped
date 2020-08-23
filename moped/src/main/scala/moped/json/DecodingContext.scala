package moped.json

import moped.cli.Environment
import moped.cli.Application

final class DecodingContext private (
    val json: JsonElement,
    val cursor: Cursor,
    val app: Application,
    val fatalUnknownFields: Boolean
) {
  def environment: Environment = app.env
  def withJson(value: JsonElement): DecodingContext =
    copy(json = value)
  def withCursor(value: Cursor): DecodingContext =
    copy(cursor = value)
  def withFatalUnknownFields(value: Boolean): DecodingContext =
    copy(fatalUnknownFields = value)

  private[this] def copy(
      json: JsonElement = this.json,
      cursor: Cursor = this.cursor,
      app: Application = this.app,
      fatalUnknownFields: Boolean = this.fatalUnknownFields
  ): DecodingContext = {
    new DecodingContext(
      json,
      cursor,
      app,
      fatalUnknownFields
    )
  }
  override def toString(): String =
    s"DecodingContext(json=${pprint.PPrinter.BlackWhite.tokenize(json).mkString}, cursor=$cursor)",
}

object DecodingContext {
  def apply(json: JsonElement): DecodingContext = {
    apply(json, Application.default)
  }
  def apply(json: JsonElement, app: Application): DecodingContext = {
    DecodingContext(json, app, NoCursor())
  }
  def apply(
      json: JsonElement,
      app: Application,
      cursor: Cursor
  ): DecodingContext = {
    new DecodingContext(
      json,
      cursor,
      app,
      fatalUnknownFields = false
    )
  }
}

package moped.json

import moped.console.Environment

final class DecodingContext private (
    val json: JsonElement,
    val cursor: Cursor,
    val environment: Environment
) {
  def withJson(value: JsonElement): DecodingContext = copy(json = value)
  def withCursor(value: Cursor): DecodingContext = copy(cursor = cursor)

  private[this] def copy(
      json: JsonElement = this.json,
      cursor: Cursor = this.cursor,
      environment: Environment = this.environment
  ): DecodingContext = {
    new DecodingContext(
      json,
      cursor,
      environment
    )
  }
  override def toString(): String =
    s"DecodingContext(json=${pprint.PPrinter.BlackWhite.tokenize(json).mkString}, cursor=$cursor)",
}

object DecodingContext {
  def apply(json: JsonElement, env: Environment): DecodingContext = {
    DecodingContext(json, env, NoCursor())
  }
  def apply(
      json: JsonElement,
      env: Environment,
      cursor: Cursor
  ): DecodingContext = {
    new DecodingContext(
      json,
      cursor,
      env
    )
  }
}

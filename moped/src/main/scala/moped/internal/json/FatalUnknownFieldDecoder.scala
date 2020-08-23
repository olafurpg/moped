package moped.internal.json

import moped.internal.diagnostics.UnknownFieldDiagnostic
import moped.json.DecodingContext
import moped.json.JsonObject
import moped.macros.ClassShaper
import moped.reporters.Diagnostic

object FatalUnknownFieldDecoder {
  def check(
      ev: ClassShaper[_],
      context: DecodingContext
  ): Option[Diagnostic] = {
    if (!context.fatalUnknownFields) return None
    val keys = context.json match {
      case JsonObject(members) =>
        members.map(_.key)
      case _ =>
        Nil
    }
    val validKeys = ev.allNames.toSet
    val invalidKeys = keys.collect {
      case name if !validKeys.contains(name.value) =>
        new UnknownFieldDiagnostic(name)
    }
    Diagnostic.fromDiagnostics(invalidKeys)
  }
}

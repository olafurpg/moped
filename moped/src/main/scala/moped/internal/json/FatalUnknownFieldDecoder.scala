package moped.internal.json

import moped.json.DecodingContext
import moped.macros.ClassShaper
import moped.json.JsonObject
import moped.reporters.Diagnostic
import moped.internal.diagnostics.UnknownFieldDiagnostic

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

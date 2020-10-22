package moped.internal.json

import moped.internal.diagnostics.UnknownFieldDiagnostic
import moped.json.DecodingContext
import moped.json.JsonObject
import moped.macros.ClassShaper
import moped.reporters.Diagnostic
import moped.json.JsonMember

object FatalUnknownFieldDecoder {
  def check(
      ev: ClassShaper[_],
      context: DecodingContext
  ): Option[Diagnostic] = {
    if (!context.fatalUnknownFields) return None
    val members: List[JsonMember] = context.json match {
      case obj: JsonObject =>
        obj.members
      case _ =>
        List()
    }
    val validKeys = ev.allNames.toSet
    val invalidKeys = members.collect {
      case member if !validKeys.contains(member.key.value) =>
        new UnknownFieldDiagnostic(member)
    }
    Diagnostic.fromDiagnostics(invalidKeys)
  }
}

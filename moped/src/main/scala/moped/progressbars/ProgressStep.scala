package moped.progressbars

import org.typelevel.paiges.Doc

final case class ProgressStep(
    static: Doc = Doc.empty,
    // Rename "active" into something more self-explanatory.
    active: Doc = Doc.empty
)

object ProgressStep {
  val empty: ProgressStep = ProgressStep(Doc.empty, Doc.empty)
}

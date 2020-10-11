package moped.progressbars

import org.typelevel.paiges.Doc

final case class ProgressStep(
    static: Doc = Doc.empty,
    active: Doc = Doc.empty
)

object ProgressStep {
  val empty = ProgressStep(Doc.empty, Doc.empty)
}

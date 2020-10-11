package moped.progressbars

import org.typelevel.paiges.Doc

final case class ProgressStep(
    static: Doc,
    active: Doc
)

object ProgressStep {
  val empty = ProgressStep(Doc.empty, Doc.empty)
}

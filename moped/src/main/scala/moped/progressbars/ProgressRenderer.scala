package moped.progressbars

import org.typelevel.paiges.Doc

class ProgressRenderer {
  def renderStart(): Doc = Doc.empty
  def renderStep(): ProgressStep = ProgressStep.empty
  def renderStop(): Doc = Doc.empty
}

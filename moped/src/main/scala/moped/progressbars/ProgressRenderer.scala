package moped.progressbars

class ProgressRenderer {
  def renderStart(): ProgressStep = ProgressStep.empty
  def renderStep(): ProgressStep = ProgressStep.empty
  def renderStop(): ProgressStep = ProgressStep.empty
}

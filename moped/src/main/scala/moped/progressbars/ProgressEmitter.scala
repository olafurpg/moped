package moped.progressbars

abstract class ProgressEmitter {
  def emit(step: ProgressStep): Unit
}

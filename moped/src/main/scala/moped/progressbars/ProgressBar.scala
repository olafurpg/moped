package moped.progressbars

abstract class ProgressBar {
  def start(): Unit
  def stop(): Unit
}

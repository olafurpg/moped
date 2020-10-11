package moped.progressbars

import java.util.concurrent.ScheduledExecutorService
import moped.reporters.Terminals

class InteractiveProgressEmitter(
    sh: ScheduledExecutorService,
    terminal: Terminals
) extends ProgressEmitter {
  private var lines = 0
  def emit(step: ProgressStep): Unit = {
    val width = terminal.screenWidth()
    val height = terminal.screenHeigth()
    // step.static.render()
    ()
  }
}

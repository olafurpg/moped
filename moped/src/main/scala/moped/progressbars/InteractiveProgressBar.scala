package moped.progressbars

import java.io.Writer
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.util.control.NonFatal

import moped.reporters.ScreenSize
import moped.reporters.Terminals
import moped.reporters.Tput

/**
 * A progress bar that renders updates at a fixed duration interval until it's canceled.
 *
 * @param out the output stream to emit the progress bar to. This progress bar should have
 *            exclusive access to the output stream between the start()/stop() methods. There
 *            are no guarantees what happens if this output stream is used concurrently while
 *            this progress bar is running.
 * @param renderer pretty-printer for this progress bar. The `renderStep()`
 *                 method runs on a single thread and should not take longer than the
 *                 `intervalDuration` time to complete.
 * @param intervalDuration the duration for which
 * @param terminal the active part of the progress bar is truncated by the screen width/height
 *                 constraints to avoid wonky output.
 * @param reportFailure callback in case of any errors, by default prints the
 *                      stack trace to `System.out`.
 */
class InteractiveProgressBar(
    out: Writer,
    renderer: ProgressRenderer,
    intervalDuration: Duration = Duration.ofMillis(20),
    terminal: Terminals = new Terminals(Tput.system),
    reportFailure: Throwable => Unit = e => e.printStackTrace(System.out)
) extends ProgressBar {

  // The `renderStep()` part runs on this single thread while the
  // `renderStart()` and `renderStop()` methods run on the callee thread.
  private val sh: ScheduledExecutorService = new ScheduledThreadPoolExecutor(1)
  InteractiveProgressBar.discardRejectedRunnables(sh)
  private implicit val ec: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(sh)

  private var printedWidth, printedHeight = 0
  private val scheduledJobs = new ConcurrentLinkedQueue[Future[_]]
  private def hasPrinted(): Boolean = printedHeight > 0 || printedWidth > 0

  private val BeforeStart = 0
  private val Active = 1
  private val AfterStop = 2
  private val state = new AtomicInteger(BeforeStart)
  def isBeforeStart(): Boolean = state.get() == BeforeStart
  def isActive(): Boolean = state.get() == Active
  def isAfterStop(): Boolean = state.get() == AfterStop

  override def start(): Unit = {
    if (state.compareAndSet(BeforeStart, Active)) {
      emitNow(ProgressStep(static = renderer.renderStart()))
      sh.scheduleAtFixedRate(
        { () =>
          try {
            if (isActive()) {
              val step = renderer.renderStep()
              emitLater(step)
            }
          } catch {
            case NonFatal(e) =>
              reportFailure(e)
          }
        },
        intervalDuration.toMillis(),
        intervalDuration.toMillis(),
        TimeUnit.MILLISECONDS
      )
    }
  }
  override def stop(): Unit = {
    // TODO: Should we guarantee `state == AfterStop` here?
    if (state.compareAndSet(Active, AfterStop)) {
      emitNow(ProgressStep(static = renderer.renderStop()))
      sh.shutdownNow()
    }
  }

  private def emitLater(step: ProgressStep): Unit = {
    cancelScheduledJobs()
    val scheduledJob = sh.submit[Unit](() => {
      emitNow(step)
    })
    scheduledJobs.add(scheduledJob)
  }
  private def emitNow(step: ProgressStep): Unit = {
    val size = terminal.screenSize()
    val static = step.static.renderTrim(size.width)
    val active = step.active.renderTrim(size.width)
    clearActivePart()
    writeStaticPart(static)
    writeActivePart(active, size)
  }

  private def writeStaticPart(static: String): Unit = {
    out.write(static)
    if (static.nonEmpty && !static.endsWith("\n")) {
      out.write("\n")
    }
    out.flush()
  }

  private def writeActivePart(active: String, size: ScreenSize): Unit = {
    if (!isActive()) return
    var i, w, h = 0
    // This while loop prints out the `active` string with truncated characters
    // outside the screen width/height constraints to avoid automatic line
    // wrapping from the terminal. When the terminal wraps text it introduces
    // synthetic lines, which we can't remove in the `clearActivePart()` method
    // because we only know how many lines have been printed via the
    // `printedHeight` variable (not how many lines the terminal has
    // synthesized).
    while (i < active.length) {
      active(i) match {
        case '\n' =>
          if (h < size.height) {
            out.write('\n')
            h += 1
            w = 0
          }
        case ch =>
          if (w < size.width) {
            out.write(ch)
            w += 1
          }
      }
      i += 1
    }
    printedWidth = w
    printedHeight = h
    out.flush()
  }

  private def clearActivePart(): Unit = {
    if (!hasPrinted()) return

    def control(n: Int, c: Char): Unit = out.write("\u001b[" + n + c)
    def up(n: Int): Unit = if (n > 0) control(n, 'A')
    def down(n: Int): Unit = if (n > 0) control(n, 'B')
    def left(n: Int): Unit = if (n > 0) control(n, 'D')
    def clearLine(): Unit = control(2, 'K')

    1.to(printedHeight).foreach { _ =>
      clearLine()
      up(1)
    }
    clearLine()
    left(printedWidth)
    out.flush()

    printedHeight = 0
    printedWidth = 0
  }

  /** Cancel old `renderStep()` calls in case the render method is slower than `durationInterval`. */
  private def cancelScheduledJobs(): Unit = {
    var job = scheduledJobs.poll()
    while (job != null) {
      job.cancel(false)
      job = scheduledJobs.poll()
    }
  }

}

object InteractiveProgressBar {
  private def discardRejectedRunnables(executor: ExecutorService): Unit =
    executor match {
      case t: ThreadPoolExecutor =>
        t.setRejectedExecutionHandler((_, _) => ())
      case _ =>
    }
}

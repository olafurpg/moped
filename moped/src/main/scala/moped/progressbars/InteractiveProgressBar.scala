package moped.progressbars

import java.util.concurrent.ScheduledExecutorService

import moped.reporters.Terminals
import java.io.Writer
import java.io.PrintWriter
import InteractiveProgressBar.Ansi
import moped.cli.CancelToken
import scala.concurrent.ExecutionContext
import moped.reporters.Tput
import java.util.concurrent.Executors
import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.util.control.NonFatal
import org.typelevel.paiges.Doc
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ScheduledThreadPoolExecutor

class InteractiveProgressBar(
    out: Writer,
    renderer: ProgressRenderer,
    intervalDuration: Duration = Duration.ofMillis(1000),
    terminal: Terminals = new Terminals(Tput.system),
    reportFailure: Throwable => Unit = e => e.printStackTrace()
) extends ProgressBar {
  private val sh: ScheduledExecutorService = new ScheduledThreadPoolExecutor(1)
  InteractiveProgressBar.discardRejectedRunnables(sh)
  private implicit val ec = ExecutionContext.fromExecutorService(sh)
  private var printed = 0
  private def clear(): Unit = {
    for (_ <- 1 to printed) {
      out.clearLine(2)
      out.up(1)
    }
    out.clearLine(2)
    out.left(10000)
    out.flush()
    printed = 0
  }
  override def start(): Unit = {
    emit(ProgressStep(static = renderer.renderStart()))
    sh.scheduleAtFixedRate(
      () => {
        try emit(renderer.renderStep())
        catch {
          case NonFatal(e) =>
            reportFailure(e)
        }
      },
      0,
      intervalDuration.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }
  override def stop(): Unit = {
    emit(ProgressStep(static = renderer.renderStop()))
    clear()
    sh.shutdownNow()
  }

  private def emit(step: ProgressStep): CancelToken = {
    CancelToken.fromJavaFuture(sh.submit[Unit] { () =>
      val size = terminal.screenSize()
      val static = step.static.renderTrim(size.width)
      clear()
      out.write(static)
      if (static.nonEmpty && !static.endsWith("\n")) {
        out.write("\n")
      }
      out.flush()
      val active = step.active.renderTrim(size.width)
      out.write(active)
      printed = active.count(_ == '\n')
      // out.write(s"<$printed> ")
      out.flush()
    })
  }
}

object InteractiveProgressBar {
  implicit class Ansi(val output: Writer) extends AnyVal {
    private def control(n: Int, c: Char): Unit =
      output.write("\u001b[" + n + c)

    /**
     * Move up `n` squares
     */
    def up(n: Int): Unit = if (n > 0) control(n, 'A')

    /**
     * Move down `n` squares
     */
    def down(n: Int): Unit = if (n > 0) control(n, 'B')

    /**
     * Move left `n` squares
     */
    def left(n: Int): Unit = if (n > 0) control(n, 'D')

    /**
     * Clear the current line
     *
     * n=0: clear from cursor to end of line
     * n=1: clear from cursor to start of line
     * n=2: clear entire line
     */
    def clearLine(n: Int): Unit =
      control(n, 'K')
  }
  private def discardRejectedRunnables(executor: ExecutorService): Unit =
    executor match {
      case t: ThreadPoolExecutor =>
        t.setRejectedExecutionHandler((_, _) => ())
      case _ =>
    }
}

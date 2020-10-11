package moped.reporters

import scala.util.Try

import org.jline.terminal.TerminalBuilder

abstract class Tput {
  def size(): Option[ScreenSize]
}

object Tput {
  def constant(w: Int): Tput = () => Some(ScreenSize(w, w))
  def constant(w: Int, h: Int): Tput = () => Some(ScreenSize(w, h))
  lazy val system: Tput = new Tput {
    val terminal = Try(TerminalBuilder.builder().build())
    def size(): Option[ScreenSize] = {
      for {
        t <- terminal
        s <- Try(t.getSize())
      } yield ScreenSize(s.getColumns(), s.getRows())
    }.toOption
  }
}

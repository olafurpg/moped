package moped.reporters

import java.nio.file.Files
import java.nio.file.Paths

import scala.sys.process.Process
import scala.util.control.NonFatal
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.collection.mutable

abstract class Tput {
  def size(): Option[ScreenSize]
}

object Tput {
  def constant(w: Int): Tput = () => Some(ScreenSize(w, w))
  def constant(w: Int, h: Int): Tput = () => Some(ScreenSize(w, h))
  val system: Tput = new Tput {
    def size(): Option[ScreenSize] = {

      val pathedTput =
        if (Files.exists(Paths.get("/usr/bin/tput"))) "/usr/bin/tput"
        else "tput"
      try {
        val columns = Process(
          Seq("sh", "-c", s"$pathedTput 2> /dev/tty")
        ).!!.trim.toInt
        val proc = Runtime.getRuntime().exec("/bin/bash")
        val out = new PrintWriter(proc.getOutputStream())
        out.println("tput -S <<!")
        out.println("cols")
        out.println("lines")
        out.println("!")
        out.close()
        val in = new BufferedReader(
          new InputStreamReader(proc.getInputStream())
        )
        val lines = mutable.ListBuffer.empty[String]
        var line = in.readLine()
        while (line != null) {
          lines += line
          line = in.readLine()
        }
        val exit = proc.waitFor()
        pprint.log(exit)
        pprint.log(lines)
        val columns2 = Process(
          Seq("sh", "-c", s"$pathedTput 2> /dev/tty")
        )
        Some(ScreenSize(columns.toInt, 80))
      } catch {
        case NonFatal(_) =>
          None
      }

    }
  }
}

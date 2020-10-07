package moped.testkit

import java.io.PrintStream

import munit.internal.console.AnsiColors
import org.junit.internal.TextListener
import org.junit.runner.notification.Failure

class MopedTextListener(writer: PrintStream = Console.out)
    extends TextListener(writer) {
  override def printFailure(
      each: Failure,
      prefix: String
  ): Unit = {
    writer.println(prefix + ") " + each.getTestHeader())
    writer.print(AnsiColors.filterAnsi(each.getTrimmedTrace()))
  }
}

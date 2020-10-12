package moped.reporters

import fansi.Attrs
import fansi.Color

sealed abstract class Severity(val name: String, val priority: Int)
    extends Ordered[Severity] {
  def compare(that: Severity): Int = {
    Integer.compare(this.priority, that.priority)
  }
  final override def toString = name
}
object Severity {
  val all: List[Severity] = 0.to(4).toList.map(fromPriority)
  def fromPriority(n: Int): Severity =
    n match {
      case 0 => SilentSeverity
      case 1 => DebugSeverity
      case 2 => InfoSeverity
      case 3 => WarningSeverity
      case 4 => ErrorSeverity
      case other => throw new IndexOutOfBoundsException(other.toString())
    }
  def color(sev: Severity): Attrs =
    sev match {
      case SilentSeverity => Color.LightMagenta
      case DebugSeverity => Color.LightGreen
      case InfoSeverity => Color.LightBlue
      case WarningSeverity => Color.LightYellow
      case ErrorSeverity => Color.LightRed
    }
}
object SilentSeverity extends Severity("silent", 0)
object DebugSeverity extends Severity("debug", 1)
object InfoSeverity extends Severity("info", 2)
object WarningSeverity extends Severity("warning", 3)
object ErrorSeverity extends Severity("error", 4)

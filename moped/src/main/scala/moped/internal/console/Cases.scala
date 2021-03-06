package moped.internal.console

object Cases {
  private val Kebab = "-([a-z])".r
  private val Camel = "([A-Z])".r
  def kebabToCamel(kebab: String): String = {
    val m = Kebab.pattern.matcher(kebab)
    val sb = new StringBuffer
    while (m.find()) {
      val replacement = m.group().charAt(1).toUpper + m.group().substring(2)
      m.appendReplacement(sb, replacement)
    }
    m.appendTail(sb)
    sb.toString
  }

  def camelToKebab(camel: String): String = {
    val m = Camel.pattern.matcher(camel)
    val sb = new StringBuffer
    while (m.find()) {
      val prefix = if (m.start() == 0) "" else "-"
      m.appendReplacement(sb, prefix + m.group().toLowerCase())
    }
    m.appendTail(sb)
    sb.toString
  }

}

package moped.internal.reporters

import org.typelevel.paiges.Doc

object Docs {
  val blankLine: Doc = Doc.line + Doc.line
  val quote: Doc = Doc.char('"')
  def quoted(d: String): Doc = quote + Doc.text(d) + quote
  val IP: Doc = Doc.text(".IP ")
  val TH: Doc = Doc.text(".TH ")
  val SH: Doc = Doc.text(".SH ")
  val PP: Doc = Doc.text(".PP ")
  val RS: Doc = Doc.line + Doc.text(".RS 4 ") + Doc.line
  val RE: Doc = Doc.text(".RE ")
}

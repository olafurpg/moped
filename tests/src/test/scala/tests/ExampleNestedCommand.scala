package tests

import moped.annotations.Inline
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.json.JsonCodec

final case class NestedOptions(
    a: Boolean = false,
    b: Boolean = false,
    c: String = "",
    d: List[String] = Nil,
    e: Boolean = true,
    g: Boolean = true
)
object NestedOptions {
  implicit val codec: JsonCodec[NestedOptions] =
    moped.macros.deriveCodec(NestedOptions())
}

final case class InlinedOptions(
    ia: Boolean = false,
    ib: Boolean = false,
    ic: String = "",
    id: List[String] = Nil,
    ie: Boolean = true,
    ig: Boolean = true
)
object InlinedOptions {
  implicit val codec: JsonCodec[InlinedOptions] =
    moped.macros.deriveCodec(InlinedOptions())
}

final case class ExampleNestedCommand(
    nested: NestedOptions = NestedOptions(),
    @Inline inlined: InlinedOptions = InlinedOptions(),
    app: Application = Application.default
) extends Command {
  def run(): Int = {
    if (nested.a)
      app.out.println(s"nested.a=${nested.a}")
    if (nested.b)
      app.out.println(s"nested.b=${nested.b}")
    if (nested.c.nonEmpty)
      app.out.println(s"nested.c=${nested.c}")
    if (nested.d.nonEmpty)
      app.out.println(s"nested.d=${nested.d.mkString(",")}")
    if (!nested.e)
      app.out.println(s"nested.e=${nested.e}")
    if (!nested.g)
      app.out.println(s"nested.g=${nested.g}")

    if (inlined.ia)
      app.out.println(s"inline.a=${inlined.ia}")
    if (inlined.ib)
      app.out.println(s"inline.b=${inlined.ib}")
    if (inlined.ic.nonEmpty)
      app.out.println(s"inline.ic=${inlined.ic}")
    if (inlined.id.nonEmpty)
      app.out.println(s"inline.id=${inlined.id.mkString(",")}")
    if (!inlined.ie)
      app.out.println(s"inline.ie=${inlined.ie}")
    if (!inlined.ig)
      app.out.println(s"inline.ig=${inlined.ig}")
    0
  }
}
object ExampleNestedCommand {
  implicit val parser: CommandParser[ExampleNestedCommand] =
    CommandParser.derive(ExampleNestedCommand())
}

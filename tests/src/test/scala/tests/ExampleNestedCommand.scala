package tests

import moped.annotations.Inline
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.json.JsonCodec
import moped.annotations.Hidden

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
    ig: Boolean = true,
    ih: Boolean = false,
    ii: String = "",
    ij: String = ""
)

object InlinedOptions {
  implicit val codec: JsonCodec[InlinedOptions] =
    moped.macros.deriveCodec(InlinedOptions())
}

final case class Inlined2Options(
    ih: Boolean = false,
    ii: String = "",
    ij: Boolean = false
)
object Inlined2Options {
  implicit val codec: JsonCodec[Inlined2Options] =
    moped.macros.deriveCodec(Inlined2Options())
}

@Hidden
final case class ExampleNestedCommand(
    nested: NestedOptions = NestedOptions(),
    @Inline inlined: InlinedOptions = InlinedOptions(),
    @Inline inlined2: Inlined2Options = Inlined2Options(),
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
    if (inlined.ih)
      app.out.println(s"inline.ih=${inlined.ih}")
    if (inlined.ii.nonEmpty)
      app.out.println(s"inline.ii=${inlined.ii}")
    if (inlined.ij.nonEmpty)
      app.out.println(s"inline.ij=${inlined.ij}")

    if (inlined2.ih)
      app.out.println(s"inline2.ih=${inlined2.ih}")
    if (inlined2.ii.nonEmpty)
      app.out.println(s"inline2.ii=${inlined2.ii}")
    if (inlined2.ij)
      app.out.println(s"inline2.ij=${inlined2.ij}")
    0
  }
}
object ExampleNestedCommand {
  implicit val parser: CommandParser[ExampleNestedCommand] =
    CommandParser.derive(ExampleNestedCommand())
}

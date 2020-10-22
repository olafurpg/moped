package tests

import moped.cli.CommandParser
import moped.cli.Command
import moped.cli.Application

final case class NestedOptions(
    a: Boolean = false,
    b: Boolean = false
)
object NestedOptions {
  implicit val codec = moped.macros.deriveCodec(NestedOptions())
}

final case class InlinedOptions(
    ia: Boolean = false,
    ib: Boolean = false
)
object InlinedOptions {
  implicit val codec = moped.macros.deriveCodec(InlinedOptions())
}

final case class ExampleNestedCommand(
    nested: NestedOptions = NestedOptions(),
    inlined: InlinedOptions = InlinedOptions(),
    app: Application = Application.default
) extends Command {
  def run(): Int = {
    app.out.println(nested.toString())
    app.out.println(inlined.toString())
    0
  }
}
object ExampleNestedCommand {
  implicit val parser = CommandParser.derive(ExampleNestedCommand())
}

package tests

import moped.annotations._
import moped.cli.Command
import moped.cli.Application
import moped.cli.CommandParser

@CommandName("example-fallback")
@Hidden
case class ExampleFallbackCommand(
    flag: Boolean = false,
    @CatchInvalidFlags
    args: List[String] = Nil,
    app: Application = Application.default
) extends Command {
  def run(): Int = {
    if (flag) app.out.println("flag=true")
    if (args.nonEmpty) app.out.println(args)
    0
  }

}

object ExampleFallbackCommand {
  implicit val parser = CommandParser.derive(ExampleFallbackCommand())
}

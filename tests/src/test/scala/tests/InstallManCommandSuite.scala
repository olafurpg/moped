package tests

import java.nio.file.Paths

import moped.annotations.Description
import moped.annotations.ExampleUsage
import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.commands._
import moped.internal.console.Utils
import moped.testkit.MopedSuite
import org.typelevel.paiges.Doc

object InstallManCommandSuite {

  @ExampleUsage("example foobar hello world")
  @Description("foobar subcommand description")
  case class FoobarCommand(
      @Description("if true, does something")
      flag: Boolean = false
  ) extends Command {
    def run(): Int = 0
  }
  object FoobarCommand {
    implicit val parser: CommandParser[FoobarCommand] = CommandParser.derive(FoobarCommand())
  }
  val app: Application = Application
    .fromName(
      "example",
      "1.0",
      List(
        CommandParser[HelpCommand],
        CommandParser[VersionCommand],
        CommandParser[ManCommand],
        CommandParser[FoobarCommand]
      )
    )
    .copy(
      tagline = "my testing binary",
      description = Doc.text("An example description"),
      examples = Doc.text("example hello world")
    )
}

class InstallManCommandSuite extends MopedSuite(InstallManCommandSuite.app) {
  test("basic") {
    runSuccessfully(List("man", "install"))
    val manFile = manDirectory.resolve("example.1")
    val man = Utils.readFile(manFile)
    Utils.overwriteFile(
      Paths
        .get(System.getProperty("user.home"))
        .resolve("man1")
        .resolve("example.1"),
      man
    )
    val rendered = scala.sys.process
      .Process(List("man", manFile.toString()))
      .!!
      .replaceAll(".\b", "")
    println(rendered)
    // assertNoDiff(
    //   rendered,
    //   """|example(1)                                                          example(1)
    //      |
    //      |
    //      |
    //      |NAME
    //      |       example
    //      |
    //      |USAGE
    //      |       example COMMAND [OPTIONS]
    //      |
    //      |COMMANDS
    //      |       help
    //      |           Print this help message
    //      |
    //      |
    //      |       version
    //      |           Print the version of this program
    //      |
    //      |
    //      |       man
    //      |           Manage man page installation and uninstallation
    //      |
    //      |
    //      |       config
    //      |
    //      |
    //      |
    //      |
    //      |
    //      |Example Manual                    2020-09-27                        example(1)
    //      |""".stripMargin
    // )
  }
}

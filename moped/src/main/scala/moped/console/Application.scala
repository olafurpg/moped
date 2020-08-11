package moped.console

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import dev.dirs.ProjectDirectories
import fansi.Color
import fansi.Str
import moped.commands.HelpCommand
import moped.internal.console.CommandLineParser
import moped.internal.diagnostics.AggregateDiagnostic
import moped.json.DecodingContext
import moped.json.DecodingResult
import moped.json.ErrorResult
import moped.json.ValueResult
import moped.macros.ClassShape
import moped.reporters.ConsoleReporter
import moped.reporters.Reporter
import moped.json.JsonObject
import moped.parsers.ConfigurationParser

case class Application(
    binaryName: String,
    version: String,
    commands: List[CommandParser[_]],
    env: Environment,
    reporter: Reporter,
    relativeCommands: List[CommandParser[_]] = Nil,
    arguments: List[String] = Nil,
    relativeArguments: List[String] = Nil,
    preProcessClassShape: ClassShape => ClassShape = HelpCommand.insertHelpFlag,
    preProcessArguments: List[String] => List[String] = { args =>
      HelpCommand.swapTrailingHelpFlag(
        HelpCommand.moveFlagsBehindSubcommand(args)
      )
    },
    onEmptyArguments: BaseCommand = new HelpCommand(),
    onNotRecognoziedCommand: BaseCommand = NotRecognizedCommand,
    parsers: List[ConfigurationParser] = Nil,
    token: CancelToken = CancelToken.empty()
) {
  require(binaryName.nonEmpty, "binaryName must be non-empty")
  def out = env.standardOutput
  def err = env.standardError
  def error(message: Str): Unit = {
    env.standardError.println(Color.LightRed("error: ") ++ message)
  }
  def warning(message: Str): Unit = {
    env.standardError.println(Color.LightYellow("warn: ") ++ message)
  }
  def info(message: Str): Unit = {
    env.standardError.println(Color.LightBlue("info: ") ++ message)
  }

  def consumedArguments: List[String] =
    arguments.dropRight(relativeArguments.length)

  def runAndExitIfNonZero(args: List[String]): Unit = {
    val exit = run(args)
    if (exit != 0) System.exit(exit)
  }
  def runSingleCommand(arguments: List[String]): Int = {
    val singleCommand = commands
      .find { c =>
        c.subcommandName != "version" &&
        c.subcommandName != "help"
      }
      .orElse(commands.headOption)
    singleCommand match {
      case Some(command) =>
        run(command.subcommandName :: arguments)
      case None =>
        this.error(
          "can't run command since the commands list is empty. " +
            "To fix this problem, update `Application.commands` field to be non-empty."
        )
        1
    }
  }
  def run(arguments: List[String]): Int = {
    Application.run(this.copy(arguments = arguments))
  }
}

object Application {
  def fromName(
      binaryName: String,
      version: String,
      commands: List[CommandParser[_]]
  ): Application = {
    val env = Environment.fromProjectDirectories(
      ProjectDirectories.fromPath(binaryName)
    )
    Application(
      binaryName,
      version,
      commands,
      env = env,
      reporter = new ConsoleReporter(env.standardOutput)
    )
  }
  def run(app: Application): Int = {
    val args = app.preProcessArguments(app.arguments)
    val base = app.copy(commands = app.commands.map(_.withApplication(app)))
    val params =
      base.commands
        .find(_.matchesName("completions"))
        .toList
        .flatMap(_.nestedCommands.map(_.parameters))
    def loop(n: Int, relativeCommands: List[CommandParser[_]]): Future[Int] = {
      val app = base.copy(
        arguments = args,
        relativeArguments = args.drop(n + 1),
        relativeCommands = relativeCommands
      )
      val remainingArguments = args.drop(n)
      remainingArguments match {
        case Nil =>
          app.onEmptyArguments.runAsFuture(app)
        case subcommand :: tail =>
          relativeCommands.find(_.matchesName(subcommand)) match {
            case Some(command) =>
              if (command.nestedCommands.nonEmpty) {
                loop(
                  n + 1,
                  command.nestedCommands
                )
              } else {
                val conf: DecodingResult[JsonObject] =
                  CommandLineParser.parseArgs[command.Value](tail)(
                    command.asClassShaper
                  )
                val configured: DecodingResult[BaseCommand] =
                  conf.flatMap(elem =>
                    command.decodeCommand(DecodingContext(elem, app.env))
                  )
                JsonObject(Nil)
                configured match {
                  case ValueResult(value) =>
                    value.runAsFuture(app)
                  case ErrorResult(error) =>
                    error.all.foreach {
                      case _: AggregateDiagnostic =>
                      case diagnostic =>
                        app.reporter.log(diagnostic)
                    }
                    Future.successful(1)
                }
              }
            case None =>
              app.onNotRecognoziedCommand.runAsFuture(app)
          }
      }
    }
    val future = loop(0, base.commands)
    val exit = future.value match {
      case Some(value) => value.get
      case None => Await.result(future, Duration.Inf)
    }
    app.out.flush()
    app.err.flush()
    exit
  }
}

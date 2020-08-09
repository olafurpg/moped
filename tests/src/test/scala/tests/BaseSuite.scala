package tests

import moped.commands.CompletionsCommand
import moped.commands.HelpCommand
import moped.commands.VersionCommand
import moped.console.Application
import moped.console.CommandParser

abstract class BaseSuite
    extends moped.testkit.MopedSuite(
      Application.fromName(
        "tests",
        "1.0.0",
        commands = List(
          CommandParser[HelpCommand],
          CommandParser[VersionCommand],
          CommandParser[CompletionsCommand],
          CommandParser[WorkingDirectoryCommand],
          CommandParser[EchoCommand]
        )
      )
    )

package tests

import moped.commands.CompletionsCommand
import moped.commands.HelpCommand
import moped.commands.VersionCommand
import moped.cli.Application
import moped.cli.CommandParser
import moped.parsers._

abstract class BaseSuite
    extends moped.testkit.MopedSuite(
      Application
        .fromName(
          "tests",
          "1.0.0",
          commands = List(
            CommandParser[HelpCommand],
            CommandParser[VersionCommand],
            CommandParser[CompletionsCommand],
            CommandParser[WorkingDirectoryCommand],
            CommandParser[EchoCommand],
            CommandParser[ConfigCommand]
          )
        )
        .copy(
          parsers = List(
            JsonParser,
            HoconParser,
            TomlParser,
            YamlParser,
            DhallParser,
            JsonnetParser
          )
        )
    ) {}

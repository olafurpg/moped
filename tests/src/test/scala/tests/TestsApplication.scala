package tests

import moped.cli.Application
import moped.cli.CommandParser
import moped.commands.CompletionsCommand
import moped.commands.HelpCommand
import moped.commands.ManCommand
import moped.commands.VersionCommand
import moped.parsers.DhallParser
import moped.parsers.HoconParser
import moped.parsers.JsonParser
import moped.parsers.JsonnetParser
import moped.parsers.TomlParser
import moped.parsers.YamlParser

object TestsApplication {
  val app: Application = Application
    .fromName(
      "tests",
      "1.0.0",
      commands = List(
        CommandParser[HelpCommand],
        CommandParser[VersionCommand],
        CommandParser[CompletionsCommand],
        CommandParser[WorkingDirectoryCommand],
        CommandParser[EchoCommand],
        CommandParser[ConfigCommand],
        CommandParser[ExampleNestedCommand],
        CommandParser[ExampleFallbackCommand],
        CommandParser[ManCommand]
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
      ),
      mockedProcesses = List(
        Application.single("zsh", app => new MockedZshCommand(app))
      )
    )

}

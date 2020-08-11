package tests

import moped.commands.CompletionsCommand
import moped.commands.HelpCommand
import moped.commands.VersionCommand
import moped.console.Application
import moped.console.CommandParser
import moped.json.JsonElement
import moped.parsers._
import moped.reporters.Input

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
    ) {
  def assertJsonEquals(obtained: JsonElement, expected: JsonElement)(implicit
      loc: munit.Location
  ): Unit =
    if (obtained != expected) {
      val width = 40
      assertNoDiff(obtained.toDoc.render(width), expected.toDoc.render(width))
      assertEquals(obtained, expected)
    }
  def parseJson(json: String): JsonElement = {
    JsonParser.parse(Input.string(json)).get
  }
}

package tests

import munit.TestOptions

class RunCompletionsCommandSuite extends BaseSuite {
  def checkCompletions(
      name: TestOptions,
      args: List[String],
      expected: List[String],
      format: String = "zsh"
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      val isEmpty = args.nonEmpty && args.last == ""
      val completeArgs = if (isEmpty) args.init else args
      val current = if (isEmpty) args.length + 1 else args.length
      val exit = app().run(
        List(
          "completions",
          "run",
          format,
          current.toString(),
          app().binaryName
        ) ++ completeArgs
      )
      assertEquals(exit, 0, clues(app.capturedOutput))
      assertEquals(app.capturedOutput.trim.linesIterator.toList, expected)
    }
  }

  def publicCommandNames: List[String] =
    List(
      "help",
      "version",
      "completions",
      "working-directory",
      "echo",
      "config"
    )

  checkCompletions(
    "empty",
    List(),
    List()
  )

  checkCompletions(
    "subcommands",
    List(""),
    publicCommandNames
  )

  checkCompletions(
    "echo-empty",
    List("echo", ""),
    List()
  )

  val allFlags: List[String] =
    List(
      "--help",
      "--no-lowercase",
      "--trailing",
      "--unchanged",
      "--uppercase"
    )

  checkCompletions(
    "echo-flag",
    List("echo", "-"),
    allFlags
  )

  checkCompletions(
    "echo-uppercase".only,
    List("echo", "--uppercase"),
    allFlags
  )

  checkCompletions(
    "echo-uppercase",
    List("echo", "--uppercase", ""),
    List()
  )
  checkCompletions(
    "help-subcommand",
    List("help", ""),
    publicCommandNames
  )

  checkCompletions(
    "help-subcommand",
    List("help", "e"),
    publicCommandNames
  )

  checkCompletions(
    "completions-subcommand",
    List("completions", "e"),
    List("help", "install", "uninstall", "run")
  )

  checkCompletions(
    "help-subcommand-repeat",
    List("help", "echo", ""),
    List()
  )

  checkCompletions(
    "path",
    List("help", "echo", ""),
    List()
  )
}

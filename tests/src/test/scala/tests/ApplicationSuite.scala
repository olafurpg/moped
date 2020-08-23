package tests

class ApplicationSuite extends BaseSuite {

  checkOutput(
    "global-flag",
    List("--uppercase", "echo", "hello world"),
    "HELLO WORLD"
  )

  checkOutput(
    "no-flag",
    List("echo", "--no-unchanged", "hello world"),
    "--no-unchanged"
  )

  checkOutput(
    "no-default-flag",
    List("echo", "--unchanged", "hello world"),
    "hello world"
  )

  checkOutput(
    "no-no-flag",
    List("echo", "--no-no-lowercase", "HELLO WORLD"),
    "hello world"
  )

  checkOutput(
    "no-flip-flag",
    List("echo", "--lowercase", "HELLO WORLD"),
    "hello world"
  )

  checkErrorOutput(
    "no-no-error",
    List("echo", "--no-no-no-lowercase", "HELLO WORLD"),
    """|error: found argument '--no-no-no-lowercase' which wasn't expected, or isn't valid in this context.
       |	Did you mean '--no-lowercase'?
       |""".stripMargin
  )

  checkOutput(
    "trailing-argument".only,
    List("echo", "hello", "--uppercase", "--", "world", "--uppercase", "--"),
    "HELLO\nworld --uppercase --"
  )
}

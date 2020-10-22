package tests

class CommandLineParserSuite extends BaseSuite {
  override def fatalUnknownFields: Boolean = true
  checkErrorOutput(
    "unexpected-flag",
    List("echo", "--foobar", "hello world"),
    """|error: found argument '--foobar' which wasn't expected, or isn't valid in this context.
       |""".stripMargin
  )

  checkOutput(
    "nested-flag".only,
    List("example-nested", "--nested.a", "--nested.b"),
    """|NestedOption(true)
       |""".stripMargin
  )

  checkOutput(
    "positional-boolean",
    List("example-nested", "--nested.a", "false"),
    """|NestedOption(true)
       |""".stripMargin
  )
}

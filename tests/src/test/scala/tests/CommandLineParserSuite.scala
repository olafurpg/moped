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
    "nested-flag",
    List("example-nested", "--nested.a", "--nested.b"),
    """|nested.a=true
       |nested.b=true
       |inline.a=false
       |inline.b=false
       |""".stripMargin
  )

  checkOutput(
    "inline-flag",
    List("example-nested", "--ia", "--ib"),
    """|nested.a=false
       |nested.b=false
       |inline.a=true
       |inline.b=true
       |""".stripMargin
  )

  checkOutput(
    "inline-nested-mix-flag",
    List("example-nested", "--ia", "--nested.a"),
    """|nested.a=true
       |nested.b=false
       |inline.a=true
       |inline.b=false
       |""".stripMargin
  )

  checkErrorOutput(
    "positional-boolean",
    List("example-nested", "--nested.a", "false"),
    """|error: unexpected positional arguments ["false"]
       |""".stripMargin
  )
  // TODO: concat arrays
}

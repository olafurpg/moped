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
       |""".stripMargin
  )

  checkOutput(
    "inline-flag",
    List("example-nested", "--ia", "--ib"),
    """|inline.a=true
       |inline.b=true
       |""".stripMargin
  )

  checkOutput(
    "inline-nested-mix-flag",
    List("example-nested", "--ia", "--nested.a"),
    """|nested.a=true
       |inline.a=true
       |""".stripMargin
  )

  checkOutput(
    "string-nested",
    List("example-nested", "--nested.c", "hello"),
    """|nested.c=hello
       |""".stripMargin
  )

  checkOutput(
    "string-inline",
    List("example-nested", "--ic", "hello"),
    """|inline.ic=hello
       |""".stripMargin
  )

  checkOutput(
    "repeated-string-nested",
    List("example-nested", "--nested.c", "hello"),
    """|nested.c=hello
       |""".stripMargin
  )

  checkOutput(
    "repeated-string-inline",
    List("example-nested", "--id", "hello1", "--id", "hello2"),
    """|inline.id=hello1,hello2
       |""".stripMargin
  )

  checkErrorOutput(
    "positional-boolean",
    List("example-nested", "--nested.a", "false"),
    """|error: unexpected positional arguments ["false"]
       |""".stripMargin
  )
  // TODO: concat arrays
  // TODO: --no- prefix
}

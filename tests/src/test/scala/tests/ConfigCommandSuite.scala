package tests

class ConfigCommandSuite extends BaseSuite {
  checkOutput(
    "basic",
    List("config", "--foobar"),
    "foobar"
  )

  checkOutput(
    "basic",
    List("config"),
    "no foobar"
  )

  checkOutput(
    "json",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.json
                                |{"foobar": true}
                                |""".stripMargin
  )

  checkErrorOutput(
    "json-error",
    List("config"),
    """|/workingDirectory/.tests.json:1:1 error: incomplete JSON
       |{
       | ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.json
                                |{
                                |""".stripMargin
  )

  checkErrorOutput(
    "json-type-error".only,
    List("config"),
    """|/workingDirectory/.tests.json:1:1 error: incomplete JSON
       |{
       | ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.json
                                |{
                                |  "foobar": "message"
                                |}
                                |""".stripMargin
  )

  checkOutput(
    "hocon",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.conf
                                |foobar = true
                                |""".stripMargin
  )

  checkErrorOutput(
    "hocon-error",
    List("config"),
    """|/workingDirectory/.tests.conf:2:0 error: Expecting a value but got wrong token: end of file
       |foobar =
       |        ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.conf
                                |foobar =
                                |""".stripMargin
  )

  checkOutput(
    "toml",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.toml
                                |foobar = true
                                |""".stripMargin
  )

  checkErrorOutput(
    "toml-error",
    List("config"),
    """|/workingDirectory/.tests.toml:1:8 error: incomplete TOML
       |foobar =
       |        ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.toml
                                |foobar =
                                |""".stripMargin
  )

  checkOutput(
    "yaml",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.yaml
                                |foobar: true
                                |""".stripMargin
  )

  checkErrorOutput(
    "yaml-error",
    List("config"),
    """|/workingDirectory/.tests.yaml:2:0 error: found unexpected end of stream
       |foobar: "
       |         ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.yaml
                                |foobar: "
                                |""".stripMargin
  )

  checkOutput(
    "dhall",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.dhall
                                |let hello = True in
                                |{ foobar = hello }
                                |""".stripMargin
  )

  checkErrorOutput(
    "dhall-error".only,
    List("config"),
    """|/workingDirectory/.tests.dhall:3:0 error: Encountered unexpected token: <EOF>. Was expecting one of: "," "}"
       |{ foobar = hel
       |              ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.dhall
                                |let hello = True in
                                |{ foobar = hel
                                |""".stripMargin
  )

  checkOutput(
    "jsonnet",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.jsonnet
                                |local hello(enabled) = {foobar: enabled};
                                |hello(true)
                                |""".stripMargin
  )

}

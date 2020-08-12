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
    "json".only,
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.json
                                |{
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

  checkOutput(
    "toml",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.toml
                                |foobar = true
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

  checkOutput(
    "dhall",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.dhall
                                |let hello = True in
                                |{ foobar = hello }
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

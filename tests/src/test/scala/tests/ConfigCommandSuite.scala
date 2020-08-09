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
    workingDirectoryLayout = """|/tests.json
                                |{"foobar": true}
                                |""".stripMargin
  )

}

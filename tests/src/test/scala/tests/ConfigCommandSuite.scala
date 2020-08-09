package tests

import moped.internal.console.Utils

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
    "json", {
      Utils.overwriteFile(workingDirectory.resolve("tests.json"))

      List("config", "--foobar")
    },
    "foobar"
  )

}

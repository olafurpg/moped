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
    "json-type-error",
    List("config"),
    """|/workingDirectory/.tests.json:2:12 error: Type mismatch at '.foobar';
       |  found    : String
       |  expected : Boolean
       |  "foobar": "message"
       |            ^
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
    """|/workingDirectory/.tests.conf:2 error: Expecting a value but got wrong token: end of file
       |foobar =
       |        ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.conf
                                |foobar =
                                |""".stripMargin
  )

  checkErrorOutput(
    "hocon-type-error",
    List("config"),
    """|/workingDirectory/.tests.conf:1 error: Type mismatch at '.foobar';
       |  found    : String
       |  expected : Boolean
       |foobar = message
       |^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.conf
                                |foobar = message
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

  checkErrorOutput(
    "toml-type-error",
    List("config"),
    """|error: Type mismatch at '.foobar';
       |  found    : String
       |  expected : Boolean
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.toml
                                |foobar = "message"
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
    """|/workingDirectory/.tests.yaml:2 error: found unexpected end of stream
       |foobar: "
       |         ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.yaml
                                |foobar: "
                                |""".stripMargin
  )

  checkErrorOutput(
    "yaml-type-error",
    List("config"),
    """|/workingDirectory/.tests.yaml:1:8 error: Type mismatch at '.foobar';
       |  found    : String
       |  expected : Boolean
       |foobar: "message"
       |        ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.yaml
                                |foobar: "message"
                                |""".stripMargin
  )

  // TODO: handle unknown fields in YAML
  // checkErrorOutput(
  //   "yaml-unknown-field",
  //   List("config"),
  //   """|/workingDirectory/.tests.yaml:1:8 error: Type mismatch;
  //      |  found    : String
  //      |  expected : Boolean
  //      |foobar: "message"
  //      |        ^
  //      |""".stripMargin,
  //   workingDirectoryLayout = """|/.tests.yaml
  //                               |foobar: "message"
  //                               |""".stripMargin
  // )

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
    "dhall-error",
    List("config"),
    """|/workingDirectory/.tests.dhall:3 error: Encountered unexpected token: <EOF>. Was expecting one of: "," "}"
       |{ foobar = hel
       |              ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.dhall
                                |let hello = True in
                                |{ foobar = hel
                                |""".stripMargin
  )

  checkErrorOutput(
    "dhall-type-error",
    List("config"),
    """|error: Type mismatch at '.foobar';
       |  found    : String
       |  expected : Boolean
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.dhall
                                |let hello = "message" in
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

  checkOutput(
    "jsonnet",
    List("config"),
    "foobar",
    workingDirectoryLayout = """|/.tests.jsonnet
                                |local hello(enabled) = {foobar: enabled};
                                |hello(true)
                                |""".stripMargin
  )

  checkErrorOutput(
    "jsonnet-error",
    List("config"),
    """|/workingDirectory/.tests.jsonnet:1:31 error: Parse error: Expected StringIn(":::", "::", ":"):1:32, found "enabled};\n"
       |local hello(enabled) = {foobar enabled};
       |                               ^
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.jsonnet
                                |local hello(enabled) = {foobar enabled};
                                |hello(true)
                                |""".stripMargin
  )

  checkErrorOutput(
    "jsonnet-type-error",
    List("config"),
    """|error: Type mismatch at '.foobar';
       |  found    : String
       |  expected : Boolean
       |""".stripMargin,
    workingDirectoryLayout = """|/.tests.jsonnet
                                |local hello(enabled) = {foobar: enabled};
                                |hello("message")
                                |""".stripMargin
  )

}

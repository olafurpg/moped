package tests

class EchoCommandSuite extends BaseSuite {
  checkOutput(
    "echo prints arguments unchanged to output",
    List("echo", "Hello World"),
    "Hello World"
  )
  checkOutput(
    "--uppercase prints output in upper-case",
    List("echo", "--uppercase", "Hello World"),
    "HELLO WORLD"
  )
  checkErrorOutput(
    "--upper does not exist".only,
    List("echo", "--upper", "Hello World"),
    """|error: found argument '--upper' which wasn't expected, or isn't valid in this context.
       |	Did you mean '--uppercase'?
       |""".stripMargin
  )
}

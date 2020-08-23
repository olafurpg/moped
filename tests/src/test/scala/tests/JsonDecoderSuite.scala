package tests

import moped.cli.Application
import moped.cli.Command
import moped.cli.CommandParser
import moped.json.DecodingContext
import moped.json.ErrorResult
import moped.json.JsonDecoder
import moped.json.JsonElement
import moped.json.ValueResult
import munit.TestOptions

case class MyClass(
    a: Int = 1
) extends Command {
  def run(app: Application): Int = 0
}
object MyClass {
  val default: MyClass = MyClass()
  implicit lazy val parser: CommandParser[MyClass] =
    CommandParser.derive[MyClass](default)
}

class JsonDecoderSuite extends BaseSuite {
  def checkDecoded(
      name: TestOptions,
      original: JsonElement,
      expected: MyClass,
      context: DecodingContext => DecodingContext = identity
  ): Unit = {
    test(name) {
      val obtained =
        JsonDecoder[MyClass].decode(context(DecodingContext(original))).get
      assertEquals(obtained, expected)
    }
  }

  def checkErrorDecoded(
      name: TestOptions,
      original: JsonElement,
      expected: String,
      context: DecodingContext => DecodingContext = identity
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      JsonDecoder[MyClass].decode(context(DecodingContext(original))) match {
        case ValueResult(value) =>
          fail(s"expected error, obtained success $value")
        case ErrorResult(error) =>
          val obtained = error.position.pretty("error", error.message)
          assertNoDiff(obtained, expected)
      }
    }
  }

  checkDecoded(
    "a",
    parseJson("{'a': 2, 'b': 42}"),
    MyClass(a = 2)
  )

  checkErrorDecoded(
    "fatal-unknown-field",
    parseJson("{'a': 2, 'b': 42}"),
    """|moped.json:1:9 error: unknown field name 'b'
       |{"a": 2, "b": 42
       |         ^
       |""".stripMargin,
    context = _.withFatalUnknownFields(true)
  )

}

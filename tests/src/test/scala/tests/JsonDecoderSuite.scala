package tests

import munit.TestOptions
import moped.json.JsonElement
import moped.console.CommandParser
import moped.json.JsonDecoder
import moped.json.DecodingContext
import moped.console.Command
import moped.console.Application

case class MyClass(
    a: Int = 1
) extends Command {
  def run(app: Application): Int = 0
}
object MyClass {
  val default = MyClass()
  implicit lazy val parser = CommandParser.derive[MyClass](default)
}

class JsonDecoderSuite extends BaseSuite {
  def checkDecoded(
      name: TestOptions,
      original: JsonElement,
      expected: MyClass
  ): Unit = {
    test(name) {
      val obtained = JsonDecoder[MyClass].decode(DecodingContext(original)).get
      assertEquals(obtained, expected)
    }
  }

  checkDecoded(
    "a",
    parseJson("{'a': 2, 'b': 42}"),
    MyClass(a = 2)
  )

  checkDecoded(
    "a",
    parseJson("{'a': 2, 'b': 42}"),
    MyClass(a = 2)
  )

}

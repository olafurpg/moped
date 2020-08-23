package tests

import moped.json.JsonSchema
import moped.annotations.Description
import moped.json.JsonEncoder
import moped.macros.ClassShaper
import munit.FunSuite

class JsonSchemaSuite extends FunSuite {

  def check[T: JsonEncoder: ClassShaper](
      name: String,
      default: T,
      expected: String
  ): Unit = {
    test(name) {
      val schema = JsonSchema.generate[T](
        "title",
        "description",
        Some("url"),
        default
      )
      val obtained = schema.get.toDoc.render(40)
      assertNoDiff(obtained, expected)
    }
  }

  case class Simple(
      @Description("A simple description")
      value: String = "default",
      @Description("A repeated field")
      repeated: Seq[Int] = Nil
  )
  object Simple {
    implicit val codec = moped.macros.deriveCodec(Simple())
  }

  check(
    "non-nested",
    Simple("Default Value", Seq(2)),
    """
      |{
      |  "$id": "url",
      |  "title": "title",
      |  "description": "description",
      |  "type": "object",
      |  "properties": {
      |    "value": {
      |      "title": "value",
      |      "description": "A simple description",
      |      "default": "Default Value",
      |      "required": false,
      |      "type": "string"
      |    },
      |    "repeated": {
      |      "title": "repeated",
      |      "description": "A repeated field",
      |      "default": [ 2 ],
      |      "required": false,
      |      "type": "array"
      |    }
      |  }
      |}
    """.stripMargin
  )

  case class B(
      @Description("My value field")
      value: String = "b"
  )
  object B {
    implicit val parser = moped.macros.deriveCodec(B())
  }
  case class A(
      value: Int = 42,
      @Description("Nested field")
      b: B = B()
  )
  object A {
    implicit val parser = moped.macros.deriveCodec(A())
  }

  check(
    "nested",
    A(42, B("Hest")),
    """
      |{
      |  "$id": "url",
      |  "title": "title",
      |  "description": "description",
      |  "type": "object",
      |  "properties": {
      |    "value": {
      |      "title": "value",
      |      "description": null,
      |      "default": 42,
      |      "required": false,
      |      "type": "number"
      |    },
      |    "b": {
      |      "title": "b",
      |      "description": "Nested field",
      |      "default": { "value": "Hest" },
      |      "required": false,
      |      "type": "object",
      |      "properties": {
      |        "value": {
      |          "title": "value",
      |          "description": "My value field",
      |          "default": "Hest",
      |          "required": false,
      |          "type": "string"
      |        }
      |      }
      |    }
      |  }
      |}
    """.stripMargin
  )

}

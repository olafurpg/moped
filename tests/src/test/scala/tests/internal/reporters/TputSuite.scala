package tests.internal.reporters

import munit.FunSuite
import moped.reporters.Tput

class TputSuite extends FunSuite {
  test("basic") {
    Tput.system.cols()
  }

}

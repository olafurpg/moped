package tests.progressbars

import munit.FunSuite
import moped.progressbars.ProgressRenderer
import org.typelevel.paiges.Doc
import moped.progressbars.ProgressStep
import moped.json.JsonArray
import moped.json.JsonNumber
import moped.progressbars.InteractiveProgressBar
import java.io.PrintWriter

class InteractiveProgressBarSuite extends FunSuite {
  test("basic") {
    val renderer = new ProgressRenderer() {
      var i = 0
      override def renderStep(): ProgressStep = {
        i += 1
        ProgressStep(
          static = Doc.text(i.toString()) + Doc.line,
          active =
            JsonArray(
              0.to(i).toList.map(i => JsonNumber(i.toDouble))
            ).toDoc + Doc.line
        )
      }
    }
    val p = new InteractiveProgressBar(new PrintWriter(System.out), renderer)
    p.start()
    Thread.sleep(10000)
    p.stop()
  }

}

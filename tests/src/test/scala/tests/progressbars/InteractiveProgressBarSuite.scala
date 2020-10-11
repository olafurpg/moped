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
      override def renderStart(): Doc = {
        Doc.text("static start")
      }
      override def renderStop(): Doc = {
        Doc.text("static stop")
      }
      override def renderStep(): ProgressStep = {
        i += 1
        val progress = ("#" * i).padTo(10, ' ')
        val bar = s"[$progress ${i.toString().padTo(2, ' ')}/10]"
        val list = JsonArray(1.to(i * 100).map(i => JsonNumber(i)).toList).toDoc
        ProgressStep(
          static = Doc.empty, //Doc.text(i.toString()),
          active = Doc.text(bar) + Doc.line + list
        )
      }
    }
    val p = new InteractiveProgressBar(new PrintWriter(System.out), renderer)
    p.start()
    Thread.sleep(10000)
    p.stop()
  }

}

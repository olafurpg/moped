package moped.reporters

class Terminals(tput: Tput) {
  def screenSize(): ScreenSize = {
    tput
      .size()
      .getOrElse(ScreenSize.default)
  }
  def screenHeigth(): Int = screenSize().width
  def screenWidth(): Int = {
    screenWidth(lowerBound = 40, upperBound = 100)
  }
  def screenWidth(lowerBound: Int, upperBound: Int): Int = {
    math.min(
      upperBound,
      math.max(lowerBound, tput.size().map(_.width).getOrElse(80) - 20)
    )
  }
}

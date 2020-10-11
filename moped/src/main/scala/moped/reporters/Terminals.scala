package moped.reporters

class Terminals(tput: Tput) {
  def screenHeigth(): Int = {
    screenWidth(lowerBound = 20, upperBound = 120)
  }
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

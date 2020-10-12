package moped.reporters

final case class ScreenSize(
    width: Int,
    height: Int
) {
  def withinWidth(
      minWidth: Int,
      maxWidth: Int
  ): ScreenSize =
    withinConstraints(
      ScreenConstraints(
        minWidth = minWidth,
        maxWidth = maxWidth,
        minHeight = height,
        maxHeight = height
      )
    )
  def withinConstraints(c: ScreenConstraints): ScreenSize =
    ScreenSize(
      width = ScreenSize.maxMin(c.minWidth, width, c.maxWidth),
      height = ScreenSize.maxMin(c.minHeight, height, c.maxHeight)
    )

}

object ScreenSize {
  val default: ScreenSize = ScreenSize(width = 120, height = 40)
  private def maxMin(min: Int, n: Int, max: Int): Int =
    math.max(math.min(n, max), min)
}

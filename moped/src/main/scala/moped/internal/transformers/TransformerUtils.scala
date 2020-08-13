package moped.internal.transformers

import upickle.core.Util

object TransformerUtils {
  def parseFloat64StringParts(
      s: CharSequence,
      decIndex: Int,
      expIndex: Int,
      index: Int
  ): Double =
    if (decIndex != -1 || expIndex != -1) s.toString.toDouble
    else Util.parseIntegralNum(s, decIndex, expIndex, index)
}

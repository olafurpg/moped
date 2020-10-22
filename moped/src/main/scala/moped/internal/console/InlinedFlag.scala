package moped.internal.console

import moped.macros.ParameterShape

final case class InlinedFlag(
    keys: List[String],
    shape: ParameterShape
)

object InlinedFlag {
  def apply(param: ParameterShape): InlinedFlag =
    InlinedFlag(param.name :: Nil, param)
}

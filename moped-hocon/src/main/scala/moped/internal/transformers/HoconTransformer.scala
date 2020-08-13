package moped.internal.transformers

import org.ekrich.config.impl.SconfigTransformerImpl
import moped.reporters.Input

class HoconTransformer(input: Input) extends SconfigTransformerImpl(input)
object HoconTransformer extends HoconTransformer(Input.none)

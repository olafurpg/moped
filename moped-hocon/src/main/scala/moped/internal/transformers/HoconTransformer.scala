package moped.internal.transformers

import moped.reporters.Input
import org.ekrich.config.impl.SconfigTransformerImpl

class HoconTransformer(input: Input) extends SconfigTransformerImpl(input)
object HoconTransformer extends HoconTransformer(Input.none)

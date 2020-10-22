package moped.cli

import moped.macros.ParameterShape
import moped.internal.console.InlinedFlag

final case class TabCompletionContext(
    shell: ShellCompletion,
    arguments: List[String],
    last: String,
    secondLast: Option[String],
    setting: Option[ParameterShape],
    allSettings: Map[String, List[InlinedFlag]],
    app: Application
)

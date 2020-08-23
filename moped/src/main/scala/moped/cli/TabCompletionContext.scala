package moped.cli

import moped.macros.ParameterShape

final case class TabCompletionContext(
    shell: ShellCompletion,
    arguments: List[String],
    last: String,
    secondLast: Option[String],
    setting: Option[ParameterShape],
    allSettings: Map[String, ParameterShape],
    app: Application
)

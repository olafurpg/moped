package moped.commands

import moped.cli.Application
import moped.cli.Command

class NestedCommand extends Command {
  def run(app: Application): Int = throw new NotImplementedError()
}

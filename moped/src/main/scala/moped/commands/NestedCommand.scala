package moped.commands

import moped.cli.Command

class NestedCommand extends Command {
  // NOTE(olafur): the run method should never get invoked because nested
  // commands can't be executed, they only delegate to subcommands.
  //
  def run(): Int = throw new NotImplementedError()
}

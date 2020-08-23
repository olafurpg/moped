---
id: getting-started
title: Getting started
---

Moped is a Scala library to build command-line applications with the following
goals:

- **Treat command-line arguments as configuration**: Moped abstracts over
  whether you're parsing command-line arguments or parsing JSON/HOCON
  configuration files. This feature allows users to declare default command-line
  flags via system-wide or project-wide configuration files to avoid repeating
  the same flags on every command invocation.
- **Model configuration as Scala data structures**: Moped allows you to manage
  user configuration options as immutable case classes and sealed traits making
  your application easy to extend and reason about.
- **Limit boilerplate where possible**: Moped provides macros that automatically
  generate encoders and decoders to help you avoid copy-pasting the same setting
  name in multiple places like the command-line parser, application
  implementation and website documentation. Copy-pasting strings is both
  cumbersome and it also increases the risk of making mistakes resulting in a
  bad end-user experience.
- **Evolve configuration without breaking changes**: it's normal that
  command-line flags and configuration options change as your application
  evolves (naming is hard, after all). Moped supports several ways to evolve
  configuration in a backwards compatible way making it easier for your users to
  upgrade to the latest version of your application.
- **Embrace tab completions**: tab completions help users stay productive when
  running your command-line application but they're often difficult to install
  and they're difficult to keep up-to-date as your application evolves. Moped
  solves this problem by providing automatic installation  of tab completion
  scripts for bash, zsh and fish on macOS, Linux and Windows.
- **Play nicely with native images**: GraalVM's `native-image` allows you to
  distribute standalone binaries for JVM applications with instant startup on
  macOS, Linux and Windows. Moped includes native-image configuration to ensure
  its classes get correctly linked at build-time or runtime by the native image
  optimizer.
- **Report helpful error messages**: Moped reports errors messages like a
  compiler, with cursors pointing to the user-written input, making it easier
  for users to troubleshoot and fix mistakes.
- **Design for testability**: Moped comes with a testkit library that makes it
  easy to write robust unit tests and integration tests for your command-line
  application that stress stateful interactions with the file system and
  standard output.

## Quick start

Let's write a small `echo` command-line tool that prints its arguments to
standard output.

### Add library dependency

```diff
  // build.sbt
  lazy val echo = project
    .settings(
+     libraryDependencies += "org.scalameta" %% "moped" % "@VERSION@"
    )
```

### Implement the `echo` command

Next, write a case class for your user configuration.

```scala mdoc
import moped.annotations._
import moped.commands._
import moped.cli._

@Description("Writes arguments to standard output")
@ExampleUsage(
  """|$ echo Hello world!
     |Hello world!
     |$ echo --uppercase Hello world!
     |HELLO WORLD!
     |""".stripMargin
)
case class EchoCommand(
  @Description("If true, prints the output in UPPERCASE")
  uppercase: Boolean = false,
  @PositionalArguments
  arguments: List[String] = Nil
) extends Command {
  def run(app: Application): Int = {
    val toPrint =
      if (uppercase) arguments.map(_.toUpperCase)
      else arguments
    app.out.println(toPrint.mkString(" "))
    0
  }
}
```

Then, write a companion object that includes the automatically generated
command-line parser and command-line application.

```scala mdoc
object EchoCommand {
  implicit lazy val parser = CommandParser.derive(EchoCommand())
  lazy val app = Application(
    binaryName = "echo",
    version = "1.0.0",
    commands = List(
      CommandParser[EchoCommand],
      CommandParser[HelpCommand],
      CommandParser[VersionCommand],
    )
  )
  def main(args: Array[String]): Unit = {
    System.exit(app.runSingleCommand(args.toList))
  }
}
```

Let's run the application manually with a few example arguments to check that it
works as expected.

```scala mdoc
EchoCommand.app.runSingleCommand(List("Hello world!"))

EchoCommand.app.runSingleCommand(List("--uppercase", "Hello world!"))

EchoCommand.app.runSingleCommand(List("--help"))

EchoCommand.app.runSingleCommand(List("--version"))
```

### Write tests for `echo` command

Manual tests are good just to get started but it's often more productive to
iterate on the code by running tests. Let's add a dependency on Moped testkit
and write tests for the `echo` application.

```diff
  // build.sbt
  lazy val echo = project
    .settings(
+     libraryDependencies += "org.scalameta" %% "moped-testkit" % "@VERSION@" % Test,
      testFrameworks += new TestFramework("munit.Framework"),
    )
```

Next, add a test suite to verify the application works as expected.

```scala mdoc
// src/test/tests/EchoSuite.scala
class EchoSuite extends moped.testkit.MopedSuite(EchoCommand.app) {
  checkOutput(
    "echo prints arguments unchanged to output",
    List("echo", "Hello World"),
    "Hello World"
  )
  checkOutput(
    "--uppercase prints output in upper-case",
    List("echo", "--uppercase", "Hello World"),
    "HELLO WORLD"
  )
  checkErrorOutput(
    "--upper does not exist",
    List("echo", "--upper", "Hello World"),
    """|error: found argument '--upper' which wasn't expected, or isn't valid in this context.
       |	Did you mean '--uppercase'?
       |""".stripMargin
  )
}
```

Run `sbt echo/test` to verify that the tests pass.

### Build `echo` native-image

For our `echo` application to be useful for other people we need some way to
share it. Let's build a native-image binary for our `echo` command using the
Moped sbt plugin.

```diff
  // project/plugins.sbt
+ addSbtPlugin("org.scalameta" % "sbt-moped" % "@VERSION@")
  // build.sbt
  lazy val echo = project
    .settings(
+     mainClass.in(Compile) := Some("echo.EchoCommand"),
      libraryDependencies += "org.scalameta" %% "moped-testkit" % "@VERSION@" % Test,
      testFrameworks += new TestFramework("munit.Framework"),
    )
+   .enablePlugins(MopedPlugin)
```

Next, run `sbt echo/mopedNativeImage` to create a native-image binary. It's
normal that this step takes a few minutes to complete.

```sh
$ sbt
> echo/mopedNativeImage
...
[/path/to/echo:91465]    classlist:   6,709.72 ms,  1.36 GB
...
[/path/to/echo:91465]      [total]:  37,170.72 ms,  2.65 GB
```

Finally, run the `echo` binary.

```sh
$ ./echo/target/moped/echo Hello world!
Hello world!
$ ./echo/target/moped/echo --uppercase Hello world!
HELLO WORLD!
```

## Inspirations

Moped is inspired by several existing libraries:

- JavaScript/TypeScript: [oclif](https://oclif.io)
- Java: [Picocli](https://picocli.info/)
- Rust: [Structopt](https://docs.rs/structopt/0.3.16/structopt/)
- Python: [Click](https://click.palletsprojects.com/en/7.x/)
- Scala: [Ammonite](http://ammonite.io/#ScalaScripts)

## Why Moped?

You may be thinking that there are so many command-line parsing libraries out
there, why create another one?

Moped was created based on my experience of building the command-line tools for
[Scalafmt](https://scalameta.org/scalafmt),
[Scalafix](https://scalacenter.github.io/scalafix),
[MDoc](https://scalameta.org/mdoc) and
[Fastpass](https://github.com/scalameta/fastpass). None of those command-line
tools currently use Moped, but they all share logic that has been copy-pasted
from project to project. The honest answer for why Moped exists is that I want
to stop copy-pasting large amounts of code every time I create a new
command-line application.

I have tried to use alternative libraries such as
[Scopt](https://github.com/scopt/scopt) (still used by Scalafmt),
[case-app](https://github.com/alexarchambault/case-app) (was used by Scalafix
for a long time) and [Picocli](https://picocli.info/). These libraries work
great for what they're advertised to do but in my particular use-cases I have
needed additional features:

- Low-boilerplate: after using scopt I observed that it required a lot of manual
  work to introduce a new command-line argument. This worked fine while I was
  the only contributor in the project but as soon as other people started
  sending PRs I noticed it took a long time to review code because changes to
  the command-line interface always required copy-pasting setting names and
  flags to multiple different places (command-line parser, implementation code,
  website documentation).
- Reasonable compile-times and comprehensible compile errors: after using
  case-app for a while I observed that it took a long time to compile my
  command-line interface and I spent a lot of time struggling to troubleshoot
  cryptic compile error messages. While I loved the low-boilerplate API of
  case-app, I still didn't feel productive.
- Unified command-line/JSON/HOCON parsing and idiomatic Scala: after playing
  around with Picocli I observed that I still nek

Here are some of the features that

Scalafi that needed similar functionality but doesn't exist currently in the
Scala ecosystem:

- Low-boilerplate:

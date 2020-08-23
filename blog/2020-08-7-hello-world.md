---
author: Ólafur Páll Geirsson
title: Moped is a new Scala library to build command-line applications
authorURL: https://twitter.com/olafurpg
authorImageURL: https://github.com/olafurpg.png
---

Hello world! I'm excited to announce the release of Moped, a new Scala library
to build command-line applications. You may be thinking that there are so many
command-line parsing libraries out there, why create another one? It's a fair
question and this post is my attempt to explain the motivation to create Moped.

## Background

The honest answer for why Moped exists is that I want to stop copy-pasting large
amounts of code every time I create a new command-line application. Moped was
created based on my experience of building the command-line tools for
[Scalafmt](https://scalameta.org/scalafmt),
[Scalafix](https://scalacenter.github.io/scalafix),
[MDoc](https://scalameta.org/mdoc) and
[Fastpass](https://github.com/scalameta/fastpass). None of those command-line
tools currently use Moped, but they all share logic that has been copy-pasted
from project to project.

I have tried to use alternative libraries such as
[Scopt](https://github.com/scopt/scopt) (still used by Scalafmt),
[case-app](https://github.com/alexarchambault/case-app) (was used by Scalafix
for a long time) and [Picocli](https://picocli.info/). These libraries work
great for what they're advertised to do but in my particular use-cases I have
always found something missingj:

- **Low-boilerplate**: after using scopt for Scalafmt I observed that it
  required a lot of manual work to introduce a new command-line argument. This
  worked fine while I was the only contributor in the project but as soon as
  other people started sending PRs I noticed it took me a long time to review
  code because changes to the command-line interface always required
  copy-pasting setting names and flags to multiple different places like the
  configuration parser and the website documentation.
- **Fast compile-times and comprehensible compile errors**: after using case-app
  for Scalafix I observed that it took a long time to compile my command-line
  interface and I spent a lot of time struggling to troubleshoot cryptic compile
  error messages. While I loved the low-boilerplate API of case-app, I didn't
  feel productive.
- **Idiomatic Scala**: after evaluating Picocli for my next command-line
  application I decided not to use it because it doesn't feel idiomatic when
  used from Scala. This is the most subjective "requirement". For example, it
  didn't feel gratisfying to provide default option values for int flags via
  strings. If you're looking to build a command-line application, you should
  seriously consider using Picocli because it looks really good.

There are many other command-line parsing libraries such as
[Scallop](https://github.com/scallop/scallop),
[twitter/util](https://github.com/twitter/util),
[Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) and
[Decline](https://github.com/bkirwi/decline). I have not used these libraries in
a serious capacity but I have heard good things about them. You should
definitely evaluate these alternatives if you're building a Scala command-line
application. But first, let me explain why the features that make Moped unique
in my opinion.

## Model commands with Scala data structures

Moped commands are implemented as normal Scala case classes that extend the
`Command` class. Below is an example command that implements an `echo`
application that prints its arguments to standard output.

```scala mdoc
import moped.annotations._
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
  @PositionalArguments()
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

Observe that command-line flags like `--uppercase` are encoded as
`uppercase: Boolean` fields on the class. Flags can have advanced types such as
`Option[T]`, `Map[String, T]`, another case class or a sealed trait.

Positional and trailing arguments are defined as case class fields just like
normal command-line flags.

## Unified command-line parsing and configuration parsing

Moped commands can be parsed from JSON instead of command-line arguments. The
benefit of JSON is that users can declare command-line arguments in
configuration files to avoid repeating the same flags on every invocation.

```sh
$ cat echo.json
{
  "uppercase": true
}
$ echo hello world!
HELLO WORLD!
```

It's often desirable to write user configuration in a more high-level syntax
than JSON so Moped additionally supports HOCON, TOML, YAML, Jsonnet and Dhall.

Error messages from decoding configuration files point to source locations in
the user-written source, just like a normal compiler.

```sh
$ cat echo.json
{
  "upper": true
}
$ echo hello world!
error: echo.json:1
HELLO WORLD!
```

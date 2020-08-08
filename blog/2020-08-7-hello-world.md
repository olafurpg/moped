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

## Model configuration as Scala data structures

A Moped command is implemented as a normal Scala case class.

```scala mdoc
println(42)
```

## Unified command-line parsing and configuration parsing

When parsing command-line arguments, Mpoed first converts the arguments into
JSON. At its core, Moped is a JSON converts command-line arguments into a JSON

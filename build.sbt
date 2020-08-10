def scala212 = "2.12.12"
def graalvm = "20.1.0"
def ujson = "1.2.0"
def isCI = "true".equalsIgnoreCase(System.getenv("CI"))
inThisBuild(
  List(
    useSuperShell := false,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.0",
    scalaVersion := scala212,
    scalafixCaching := true,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions ++= List(
      "-Ywarn-unused:imports",
      "-Yrangepos"
    )
  )
)

crossScalaVersions := Nil
skip.in(publish) := true

lazy val moped = project
  .settings(
    libraryDependencies ++= List(
      // The GraalVM native-image dependency is necessary to avoid runtime
      // exceptions when running native binaries. The dependency is
      // "compile-internal" because downstream users don't need it on their
      // compile or runtime classpaths.
      "org.graalvm.nativeimage" % "svm" % graalvm % "compile-internal",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "dev.dirs" % "directories" % "20",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2",
      "com.lihaoyi" %% "ujson" % ujson,
      "com.lihaoyi" %% "pprint" % "0.5.9",
      "com.lihaoyi" %% "fansi" % "0.2.7",
      "org.typelevel" %% "paiges-core" % "0.3.1"
    )
  )

lazy val hocon = project
  .in(file("moped-hocon"))
  .settings(
    moduleName := "moped-hocon",
    libraryDependencies ++= List(
      "org.ekrich" %% "sconfig" % "1.3.1"
    )
  )
  .dependsOn(moped)

lazy val yaml = project
  .in(file("moped-yaml"))
  .settings(
    moduleName := "moped-yaml",
    libraryDependencies ++= List(
      "org.yaml" % "snakeyaml" % "1.26"
    )
  )
  .dependsOn(moped)

lazy val testkit = project
  .settings(
    libraryDependencies ++= List(
      "org.scalameta" %% "munit" % "0.7.10"
    )
  )
  .dependsOn(moped)

lazy val tests = project
  .settings(
    skip.in(publish) := true,
    testFrameworks := List(new TestFramework("munit.Framework")),
    buildInfoPackage := "tests",
    buildInfoKeys := Seq[BuildInfoKey](
      "expectDirectory" -> sourceDirectory.in(Test).value./("expect")
    ),
    mainClass.in(Compile) := Some("tests.EchoCommand"),
    mopedNativeImageOptions ++= List(
      "--initialize-at-build-time",
      "--report-unsupported-elements-at-runtime"
    )
  )
  .enablePlugins(BuildInfoPlugin, MopedPlugin)
  .dependsOn(testkit)

val scalatagsVersion = Def.setting {
  if (scalaVersion.value.startsWith("2.11")) "0.6.7"
  else "0.7.0"
}

lazy val plugin = project
  .in(file("moped-sbt"))
  .settings(
    sbtPlugin := true,
    moduleName := "sbt-moped",
    crossScalaVersions := List(scala212),
    buildInfoPackage := "sbtmoped",
    buildInfoKeys := Seq[BuildInfoKey](
      version
    )
  )
  .enablePlugins(BuildInfoPlugin)

lazy val docs = project
  .in(file("moped-docs"))
  .settings(
    moduleName := "moped-docs",
    fork := isCI,
    libraryDependencies ++= List(
      "com.lihaoyi" %% "scalatags" % scalatagsVersion.value
    ),
    mdocVariables := Map(
      "VERSION" -> version.value.replaceFirst("\\+.*", ""),
      "SCALA_VERSION" -> scalaVersion.value
    ),
    mdocOut :=
      baseDirectory.in(ThisBuild).value / "website" / "target" / "docs",
    mdocExtraArguments := {
      val cwd = baseDirectory.in(ThisBuild).value
      List(
        "--no-link-hygiene",
        "--in",
        (cwd / "docs").getAbsolutePath,
        "--out",
        (cwd / "website" / "target" / "docs").getAbsolutePath,
        "--in",
        (cwd / "blog").getAbsolutePath,
        "--out",
        (cwd / "website" / "blog").getAbsolutePath
      )
    }
  )
  .dependsOn(tests)
  .enablePlugins(DocusaurusPlugin)

addCommandAlias("native-image", "tests/mopedNativeImage")

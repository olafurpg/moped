def scala212 = "2.12.12"
def graalvm = "20.1.0"
def ujson = "1.2.0"
def isCI = "true".equalsIgnoreCase(System.getenv("CI"))
inThisBuild(
  List(
    organization := "org.scalameta",
    homepage := Some(url("https://github.com/scalameta/moped")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@gmail.com",
        url("https://geirsson.com")
      )
    ),
    useSuperShell := false,
    scalaVersion := scala212,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.0",
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
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "dev.dirs" % "directories" % "20",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2",
      "com.lihaoyi" %% "os-lib" % "0.7.1",
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

lazy val toml = project
  .in(file("moped-toml"))
  .settings(
    moduleName := "moped-toml",
    libraryDependencies ++= List(
      "tech.sparse" %% "toml-scala" % "0.2.2"
    )
  )
  .dependsOn(moped)

lazy val dhall = project
  .in(file("moped-dhall"))
  .settings(
    moduleName := "moped-dhall",
    libraryDependencies ++= List(
      "org.dhallj" %% "dhall-scala" % "0.5.0-M1"
    )
  )
  .dependsOn(moped)

lazy val jsonnet = project
  .in(file("moped-jsonnet"))
  .settings(
    moduleName := "moped-jsonnet",
    libraryDependencies ++= List(
      "com.lihaoyi" %% "sjsonnet" % "0.2.6"
    )
  )
  .dependsOn(moped)

lazy val testkit = project
  .in(file("moped-testkit"))
  .settings(
    moduleName := "moped-testkit"
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
    nativeImageOptions ++= List(
      "--initialize-at-build-time",
      "--report-unsupported-elements-at-runtime"
    )
  )
  .enablePlugins(BuildInfoPlugin, NativeImagePlugin)
  .dependsOn(testkit, hocon, toml, yaml, dhall, jsonnet)

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
addCommandAlias("scalafixCheckAll", "scalafixAll --check")

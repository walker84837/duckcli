import Dependencies._

ThisBuild / scalaVersion     := "3.3.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.winlogon"
ThisBuild / organizationName := "winlogon"
Compile / mainClass := Some("org.winlogon.Main")

// GitHub CI
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"), JavaSpec.temurin("21"))
ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest", "windows-latest", "macos-latest")
ThisBuild / publishTo := None
publish / skip := true

crossScalaVersions := Seq("3.3.4")

lazy val root = (project in file("."))
  .settings(
    name := "duckcli",
  )

// Merge strategy for avoiding conflicts in dependencies
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

assembly / mainClass := Some("org.winlogon.Main")

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.10.1",
  "com.softwaremill.sttp.client3" %% "circe" % "3.10.1",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.10.1",
  "io.circe" %% "circe-core" % "0.14.10",
  "io.circe" %% "circe-parser" % "0.14.10",
  "org.jline" % "jline" % "3.26.3",
  "org.jline" % "jline-reader" % "3.26.0",
  "org.jline" % "jline-terminal" % "3.26.0",
  "ch.qos.logback" % "logback-classic" % "1.2.10",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
)

// import Dependencies._

ThisBuild / scalaVersion     := "2.13.14"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.winlogon"
ThisBuild / organizationName := "winlogon"
Compile / mainClass := Some("org.winlogon.Main")

// GitHub CI
ThisBuild / githubWorkflowJavaVersions += JavaSpec.temurin("17")
ThisBuild / publishTo := None
publish / skip := true

crossScalaVersions := Seq("2.12.20", "2.13.15")

lazy val root = (project in file("."))
  .settings(
    name := "duckcli",
  )

// Merge strategy for avoiding conflicts in dependencies
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

mainClass in assembly := Some("org.winlogon.Main")

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.10.1",
  "com.softwaremill.sttp.client3" %% "circe" % "3.10.1",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.10.1",
  "io.circe" %% "circe-core" % "0.14.10",
  "io.circe" %% "circe-parser" % "0.14.10",
  "org.jline" % "jline" % "3.26.3",
  "org.jline" % "jline-reader" % "3.26.0",
  "org.jline" % "jline-terminal" % "3.26.0"
)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

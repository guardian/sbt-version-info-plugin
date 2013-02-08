name := "sbt-version-info-plugin"

organization := "com.gu"

sbtPlugin := true

scalaVersion := "2.10.0"

crossScalaVersions ++= Seq("2.9.1", "2.9.2")

releaseSettings

publishTo := Some(Resolver.url("scala-sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false


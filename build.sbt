name := "sbt-version-info-plugin"

organization := "com.gu"

sbtPlugin := true

publishTo := Some(Resolver.url("scala-sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false


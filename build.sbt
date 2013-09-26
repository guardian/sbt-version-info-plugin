name := "sbt-version-info-plugin"

organization := "com.gu"

sbtPlugin := true

releaseSettings

publishTo := Some(Resolver.url("scala-sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false

crossBuildingSettings

CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")
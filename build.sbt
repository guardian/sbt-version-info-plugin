name := "sbt-version-info-plugin"

organization := "com.gu"

version := "2.3-SNAPSHOT"

sbtPlugin := true

resolvers += Classpaths.typesafeResolver

libraryDependencies += "net.liftweb" %% "lift-json" % "2.4-M4"

seq(scalariformSettings: _*)
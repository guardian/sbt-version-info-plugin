name := "sbt-version-info-plugin"

organization := "com.gu"

version := "2.3-SNAPSHOT"

sbtPlugin := true

resolvers += "repo.codahale.com" at "http://repo.codahale.com"

libraryDependencies += "com.codahale" %% "jerkson" % "0.5.0"

seq(scalariformSettings: _*)
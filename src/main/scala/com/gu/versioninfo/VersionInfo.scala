package com.gu.versioninfo

import java.net.InetAddress
import java.util.Date
import sbt._
import Keys._
import java.lang.System

object VersionInfo extends Plugin {

  val branch = SettingKey[String]("version-branch")
  val buildNumber = SettingKey[String]("version-build-number")
  val vcsNumber = SettingKey[String]("version-vcs-number")
  val versionTxtFile = SettingKey[File]("version-txt-file", "Location to generate the version.txt file in")


  override val settings = Seq(
    branch := "trunk",
    buildNumber := System.getProperty("build.number", "DEV"),
    vcsNumber := System.getProperty("build.vcs.number", "DEV"),
    versionTxtFile :=  file(resourceManaged) / "version.txt".
    resourceGenerators in Compile <+= (versionTxtFile, branch, buildNumber, vcsNumber, streams) map buildFile
  )

  def buildFile(versionFile: File, branch: String, buildNum: String, vcsNum: String, s: TaskStreams) = {
    val versionInfo = Map(
      "Revision" -> vcsNum,
      "Build" -> buildNum,
      "Date" -> new Date().toString,
      "Built-By" -> System.getProperty("user.name", "<unknown>"),
      "Built-On" -> InetAddress.getLocalHost.getHostName)

    val versionFileContents = versionInfo.map{ case (x, y) => x + ": " + y }.toList.sorted

    s.log.debug("Writing to " + versionFile + ":\n   " + versionFileContents.mkString("\n   "))

    IO.write(versionFile, versionFileContents mkString ("\n") )

    Seq(versionFile)
  }

}

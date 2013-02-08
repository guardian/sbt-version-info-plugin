package com.gu.versioninfo

import java.net.InetAddress
import java.util.{Properties, Date}
import sbt._
import Keys._
import java.lang.System
import scala.collection.JavaConversions._

object VersionInfo extends Plugin {

  implicit def string2Dequote(s: String) = new {
    lazy val dequote = s.replace("\"", "")
  }

  val buildNumber = SettingKey[String]("version-build-number")
  val vcsBranch = SettingKey[String]("version-branch")
  val vcsNumber = SettingKey[String]("version-vcs-number")
  val vcsUrl = SettingKey[String]("version-vcs-url")
  val properties = SettingKey[Properties]("all-properties")

  override val settings = Seq(
    buildNumber := System.getProperty("build.number", "DEV"),
    vcsBranch := System.getProperty("teamcity.build.branch", "DEV"),
    vcsNumber := System.getProperty("build.vcs.number", "DEV"),
    vcsUrl := System.getProperty("vcsroot.url", ""),
    properties := System.getProperties,
    resourceGenerators in Compile <+= (resourceManaged in Compile, name, vcsUrl, vcsBranch, vcsNumber, buildNumber, streams, properties) map buildFile
  )

  def buildFile(outDir: File, name: String, vcsUrl: String, branch: String, vcsNum: String, buildNum: String, s: TaskStreams, props: Properties) = {
    s.log.info("Known properties: %s" format props.toMap.toString())

    val versionInfo = Map(
      "Revision" -> vcsNum.dequote.trim,
      "Branch" -> branch.dequote.trim,
      "Build" -> buildNum.dequote.trim,
      "Date" -> new Date().toString,
      "Built-By" -> System.getProperty("user.name", "<unknown>"),
      "Built-On" -> InetAddress.getLocalHost.getHostName)

    val versionFileContents = (versionInfo map { case (x, y) => x + ": " + y }).toList.sorted

    val versionFile = outDir / "versioninfo"/ ("%s.json" format name)
    s.log.debug("Writing to " + versionFile + ":\n   " + versionFileContents.mkString("\n   "))

    IO.write(versionFile, versionFileContents mkString "\n")

    Seq(versionFile)
  }
}

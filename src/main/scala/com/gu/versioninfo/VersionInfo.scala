package com.gu.versioninfo

import java.net.InetAddress
import java.util.Date
import sbt._
import Keys._
import java.lang.System
import com.codahale.jerkson.Json._

object VersionInfo extends Plugin {

  val branch = SettingKey[String]("version-branch")
  val buildNumber = SettingKey[String]("version-build-number")
  val vcsNumber = SettingKey[String]("version-vcs-number")
  val vcsRootUrl = SettingKey[String]("version-vcsroot-url")
  val vcsRootBranch = SettingKey[String]("version-vcsroot-branch")

  override val settings = Seq(
    buildNumber := System.getProperty("build.number", "DEV"),
    branch := System.getProperty("build.vcs.branch", "DEV"),
    vcsNumber := System.getProperty("build.vcs.number", "DEV"),
    vcsRootUrl := System.getProperty("vcsroot.url", "DEV"),
    vcsRootBranch := System.getProperty("vcsroot.branch", "DEV"),

    resourceGenerators in Compile <+= (resourceManaged in Compile, branch, buildNumber, vcsNumber, streams) map buildFile,
    resourceGenerators in Compile <+= (resourceManaged in Compile, buildNumber, vcsNumber, vcsRootUrl, vcsRootBranch, streams, name) map buildJsonFile
  )

  def buildFile(outDir: File, branch: String, buildNum: String, vcsNum: String, s: TaskStreams) = {
    val versionInfo = Map(
      "Revision" -> vcsNum,
      "Branch" -> branch,
      "Build" -> buildNum,
      "Date" -> new Date().toString,
      "Built-By" -> System.getProperty("user.name", "<unknown>"),
      "Built-On" -> InetAddress.getLocalHost.getHostName)

    val versionFileContents = (versionInfo map { case (x, y) => x + ": " + y }).toList.sorted

    val versionFile = outDir / "version.txt"
    s.log.debug("Writing to " + versionFile + ":\n   " + versionFileContents.mkString("\n   "))

    IO.write(versionFile, versionFileContents mkString "\n")

    Seq(versionFile)
  }

  def buildJsonFile(outDir: File, buildNum: String, vcsNum: String, vcsUrl: String, vcsBranch: String, s: TaskStreams, projectName: String) = {
    case class Git(url: String, branch: String, commit: String) {
      val `type`: String = "git"
    }

    case class Environment(
      `built-by`: Option[String],
      `built-on`: Option[String],
      `built-jdk`: Option[String])

    case class Version(
      label: String,
      release: String,
      build: String,
      date: String,
      `version-control`: Git,
      description: Option[String],
      environment: Option[Environment])

    val environment = Environment(None, None, None)
    val git = Git(vcsUrl, vcsBranch, vcsNum)
    val version = Version("", "", buildNum, new Date().toString, git, None, None)
    val jsonContent = generate(version)

    val versionJsonFile = outDir / ("%s.version.json" format projectName)
    s.log.debug("Writing to %s:\n   %s" format (versionJsonFile, jsonContent))

    IO.write(versionJsonFile, jsonContent)
    Seq(versionJsonFile)
  }
}

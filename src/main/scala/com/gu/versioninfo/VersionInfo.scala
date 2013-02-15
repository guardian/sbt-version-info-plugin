package com.gu.versioninfo

import java.net.InetAddress
import java.util.{Properties, Date}
import sbt._
import Keys._
import java.lang.System
import scala.collection.JavaConversions._
import java.io.FileInputStream
import util.parsing.json.JSONObject

object VersionInfo extends Plugin {

  implicit def string2Dequote(s: String) = new {
    lazy val dequote = s.replace("\"", "")
  }

  def getSystemProperty(key: String): Option[String] = {
    try {
      Some(System.getProperty(key))
    } catch {
      case e:NullPointerException => None
      case e:IllegalArgumentException => None
    }
  }

  def readTeamCityProperties(buildProperties: File): Map[String, String] = {
    try {
      val tcPropFile = System.getProperty("build.properties.file")
      val tcProperties = new Properties()
      tcProperties.load(new FileInputStream(new File(tcPropFile)))
      val tcConfigFile = tcProperties.getProperty("teamcity.configuration.properties.file")
      tcProperties.load(new FileInputStream(new File(tcConfigFile)))
      tcProperties.toMap
    } catch {
      case e:Exception =>
        s.log.warn("TeamCity properties file not found - not in TeamCity?")
        Map.empty
    }
  }

  val versionInfoDev = SettingKey[Boolean]("version-info-dev")
  val teamcityBuildProperties = SettingKey[Option[File]]("teamcity-build-properties")

  val projectDescription = SettingKey[Option[String]]("project-description")
  val ciBuildNumber = SettingKey[Option[String]]("ci-build-number")
  val vcsRevision = SettingKey[Option[String]]("vcs-revision")
  val vcsBranch = SettingKey[Option[String]]("vcs-revision")

  override val settings = {
    Seq(
      teamcityBuildProperties := getSystemProperty("build.properties.file").map(new File),
      ciBuildNumber := getSystemProperty("build.number"),
      vcsBranch := getSystemProperty("build.vcs.branch"),
      vcsRevision := getSystemProperty("build.vcs.number"),
      projectDescription := None,

      versionInfoDev := ( getSystemProperty("build.properties.file").isEmpty && getSystemProperty("build.number").isEmpty
      && getSystemProperty("build.branch").isEmpty && getSystemProperty("build.vcs.number").isEmpty ),

      resourceGenerators in Compile <+= (
        resourceManaged in Compile,
        streams,
        name,
        versionInfoDev,
        teamcityBuildProperties,
        ciBuildNumber,
        vcsRevision,
        vcsBranch,
        projectDescription
      ) map buildFile
    )
  }

  def buildFile(outDir: File, s: TaskStreams, name: String, versionInfoDev: Boolean,
                teamcityBuildProperties: Option[File], ciBuildNumber: Option[String], vcsRevision: Option[String],
                vcsBranch: Option[String], projectDescription: Option[String]) = {

    val tcProps = teamcityBuildProperties.map(readTeamCityProperties).getOrElse(Map.empty)



    val versionInfo = tcProps ++ Map(
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

  def buildJson(outDir: File, buildNum: String, vcsNum: String, vcsUrl: String, vcsBranch: String, s: TaskStreams, projectName: String, versionName: String): String = {

    val ciMap = Map(
      "built-by" -> None,
      "built-on" -> None,
      "build-jdk" -> None,
      "build-id" -> None,
      "build-number" -> None,
      "name" -> None,
      "project" -> None,
      "configuration" -> None
    )
    val vcsMap = Map(
      "type" -> "git",
      "revision" -> vcsNum,
      "branch" -> vcsBranch,
      "url" -> vcsUrl
    )
    val projectMap = Map(
      "label" -> "",
      "description" -> versionName,
      "ci" -> ciMap,
      "vcs" -> vcsMap
    )

    val json = JSONObject(Map(projectName -> projectMap))
    val jsonContent = json.toString()

    val versionJsonFile = outDir / ("%s.version.json" format projectName)
    s.log.debug("Writing to %s:\n   %s" format (versionJsonFile, jsonContent))

    IO.write(versionJsonFile, jsonContent)
    Seq(versionJsonFile)
  }
}

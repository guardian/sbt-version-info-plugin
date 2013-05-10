package com.gu.versioninfo

import java.net.InetAddress
import java.util.{Properties, Date}
import sbt._
import sbt.Keys._
import java.lang.System
import scala.collection.JavaConversions._
import java.io.FileInputStream
import scala.util.parsing.json.JSONObject
import scala.Some

object VersionInfo extends Plugin {

  val DEV = "DEV"

  implicit def string2Dequote(s: String) = new {
    lazy val dequote = s.replace("\"", "").replaceAll("""\\/""", "/")
  }

  def getSystemProperty(key: String): Option[String] = {
    try {
      Some(System.getProperty(key))
    } catch {
      case e:NullPointerException => None
      case e:IllegalArgumentException => None
    }
  }

  def readTeamCityProperties(tcPropFile: Option[File], s: TaskStreams): Map[String, String] = {
    val tcProperties = tcPropFile.flatMap { file =>
      try {
        val props = new Properties()
        props.load(new FileInputStream(file))
        Some(props)
      } catch {
        case e:Exception =>
          s.log.info("Couldn't read TeamCity build properties file from %s" format file)
          None
      }
    }

    tcProperties.flatMap { props =>
      val tcConfigFile = Option(props.getProperty("teamcity.configuration.properties.file"))
      try {
        tcConfigFile.foreach(file => props.load(new FileInputStream(new File(file))))
        Some(props.toMap)
      } catch {
        case e:Exception =>
          s.log.info("Couldn't read TeamCity configuration properties file from %s" format tcConfigFile)
          None
        }
    }.getOrElse(Map.empty).mapValues(_.dequote)
  }

  val teamcityBuildPropertiesFile = SettingKey[Option[File]]("teamcity-build-properties-file")
  val teamcityBuildProperties = TaskKey[Map[String,String]]("teamcity-build-properties")
  val branch = SettingKey[String]("version-branch")
  val buildNumber = SettingKey[String]("version-build-number")
  val vcsNumber = SettingKey[String]("version-vcs-number")

  override val settings = Seq(
    teamcityBuildPropertiesFile := getSystemProperty("build.properties.file").map(fileName => new File(fileName)),
    teamcityBuildProperties <<= (teamcityBuildPropertiesFile, streams) map { readTeamCityProperties },
    buildNumber := System.getProperty("build.number", DEV),
    branch := System.getProperty("build.vcs.branch", DEV),
    vcsNumber := System.getProperty("build.vcs.number", DEV),
    resourceGenerators in Compile <+= (resourceManaged in Compile, branch, buildNumber, vcsNumber, teamcityBuildProperties, streams) map buildFile,
    resourceGenerators in Compile <+= (resourceManaged in Compile, teamcityBuildProperties, name, description, streams) map buildJson
  )

  def buildFile(outDir: File, branch: String, buildNum: String, vcsNum: String, tcProps:Map[String,String], s: TaskStreams) = {
    val tcMap = if (tcProps.isEmpty) {
      Map("Revision" -> vcsNum.dequote.trim,
        "Branch" -> branch.dequote.trim,
        "Build" -> buildNum.dequote.trim)
    } else {
      Map(
        "Revision" -> tcProps.getOrElse("build.vcs.number","<unknown>"),
        "Branch" -> tcProps.getOrElse("teamcity.build.branch","<unknown>"),
        "Build" -> tcProps.getOrElse("build.number","<unknown>"))
    }

    val versionInfo = tcMap ++ Map(
      "Date" -> new Date().toString,
      "Built-By" -> System.getProperty("user.name", "<unknown>"),
      "Built-On" -> InetAddress.getLocalHost.getHostName)

    val versionFileContents = (versionInfo map { case (x, y) => x + ": " + y }).toList.sorted

    val versionFile = outDir / "version.txt"
    s.log.debug("Writing to " + versionFile + ":\n   " + versionFileContents.mkString("\n   "))

    IO.write(versionFile, versionFileContents mkString "\n")

    Seq(versionFile)
  }

  def buildJson(outDir: File, tcProps:Map[String,String], projectName: String, description: String, s: TaskStreams) = {

    def jsonMap(input:Map[String, Any]) = {
      JSONObject(input.filter(_._2 != None).mapValues{
        case Some(value) => value
        case other => other
      })
    }

    val ciJson = jsonMap(Map(
      "provider" -> "teamcity",
      "built-by" -> System.getProperty("user.name", "<unknown>"),
      "built-on" -> InetAddress.getLocalHost.getHostName,
      "build-jdk" -> None,
      "build-id" -> tcProps.get("teamcity.build.id"),
      "build-number" -> tcProps.get("build.number"),
      "name" -> None,
      "project" -> tcProps.get("teamcity.projectName"),
      "configuration" -> tcProps.get("teamcity.buildConfName"),
      "url" -> {
        val serverUrl = tcProps.get("teamcity.serverUrl")
        val buildId = tcProps.get("teamcity.build.id")
        if (serverUrl.isDefined && buildId.isDefined) {
          Some("%s/viewLog.html?buildId=%s" format (serverUrl.get,buildId.get))
        } else None
      }
    ))
    val vcsJson = jsonMap(Map(
      "provider" -> "git",
      "revision" -> tcProps.get("build.vcs.number"),
      "branch" -> tcProps.get("teamcity.build.branch"),
      "url" -> tcProps.get("vcsroot.url")
    ))
    val projectJson = jsonMap(Map(
      "label" -> "",
      "description" -> description,
      "ci" -> ciJson,
      "vcs" -> vcsJson,
      "date" -> new Date().getTime,
      "human-readable-date" -> new Date().toString
    ))

    val json = jsonMap(Map(projectName -> projectJson))
    val jsonContent = json.toString()

    val versionJsonFile = outDir / ("%s.version.json" format projectName)
    s.log.debug("Writing to %s:\n   %s" format (versionJsonFile, jsonContent))

    IO.write(versionJsonFile, jsonContent)
    Seq(versionJsonFile)
  }
}

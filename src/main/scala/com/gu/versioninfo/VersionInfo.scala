package com.gu.versioninfo

import java.net.InetAddress
import java.util.{Properties, Date}
import sbt._
import Keys._
import java.lang.System
import scala.collection.JavaConversions._
import java.io.FileInputStream

object VersionInfo extends Plugin {

  implicit def string2Dequote(s: String) = new {
    lazy val dequote = s.replace("\"", "")
  }

  val buildNumber = SettingKey[String]("version-build-number")
  val vcsBranch = SettingKey[String]("version-branch")
  val vcsNumber = SettingKey[String]("version-vcs-number")
  val vcsUrl = SettingKey[String]("version-vcs-url")

  override val settings = Seq(
    buildNumber := System.getProperty("build.number", "DEV"),
    vcsBranch := System.getProperty("teamcity.build.branch", "DEV"),
    vcsNumber := System.getProperty("build.vcs.number", "DEV"),
    vcsUrl := System.getProperty("vcsroot.url", ""),

    resourceGenerators in Compile <+= (resourceManaged in Compile, name, vcsUrl, vcsBranch, vcsNumber, buildNumber, streams) map buildFile
  )

  def buildFile(outDir: File, name: String, vcsUrl: String, branch: String, vcsNum: String, buildNum: String, s: TaskStreams) = {
    val tcProps = try {
      val tcPropFile = System.getProperty("build.properties.file")
      val tcProperties = new Properties()
      tcProperties.load(new FileInputStream(new File(tcPropFile)))
      tcProperties.toMap
    } catch {
      case e:Exception =>
        s.log.warn("TeamCity properties file not found - not in TeamCity?")
        Map.empty
    }

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
}

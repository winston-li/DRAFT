import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._

object Common {
  val settings = Seq(
    organization := "com.compal",
    version := "0.1.0",
    scalaVersion := Version.scala,
    scalacOptions ++= Seq("-encoding", "UTF-8", s"-target:jvm-${Version.jdk}", "-feature", "-language:_", "-deprecation",
      "-unchecked", "-Xfatal-warnings", "-Xlint"),
    mergeStrategy in assembly := {
      case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
      case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
      case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
      case m if m.toLowerCase.endsWith("eclipsef.rsa")         => MergeStrategy.discard
      case "log4j.properties"                                  => MergeStrategy.concat
      case "reference.conf"                                    => MergeStrategy.concat
      case _                                                   => MergeStrategy.first
    }
  )
}


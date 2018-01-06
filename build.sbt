import sbtcrossproject.{crossProject, CrossType}

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.12", "2.12.4")

val rawVersion = "0.5.0"
val sharedSettings = Seq(
  organization := "com.nthportal",
  name := "convert",
  description := "A Scala library for handling conversions between types by throwing exceptions or returning Options " +
    "containing the results.",

  isSnapshot := false,
  version := rawVersion + { if (isSnapshot.value) "-SNAPSHOT" else "" },

  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.12", "2.12.4"),

  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.1+" % Test
  ),

  autoAPIMappings := true,

  scalacOptions ++= {
    if (isSnapshot.value) Seq()
    else scalaVersion.value split '.' map { _.toInt } match {
      case Array(2, 11, _) => Seq("-optimize")
      case Array(2, 12, minor) if minor <= 2 => Seq("-opt:l:project")
      case Array(2, 12, minor) if minor > 2 => Seq("-opt:l:inline")
      case _ => Seq()
    }
  },

  publishTo := {
    if (isSnapshot.value) Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
    else None
  },

  publishMavenStyle := true,
  licenses := Seq("The Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/NthPortal/convert")),

  pomExtra := {
    <scm>
      <url>https://github.com/NthPortal/convert</url>
      <connection>scm:git:git@github.com:NthPortal/convert.git</connection>
      <developerConnection>scm:git:git@github.com:NthPortal/convert.git</developerConnection>
    </scm>
      <developers>
        <developer>
          <id>NthPortal</id>
          <name>NthPortal</name>
          <url>https://github.com/NthPortal</url>
        </developer>
      </developers>
  }
)

lazy val convert = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(sharedSettings)

lazy val convertJVM = convert.jvm
lazy val convertJS = convert.js

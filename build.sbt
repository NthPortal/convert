organization := "com.nthportal"
name := "convert"
description := "A Scala library for handling conversions between types by throwing exceptions or returning Options " +
  "containing the results."

val rawVersion = "0.3.0"
isSnapshot := false
version := rawVersion + { if (isSnapshot.value) "-SNAPSHOT" else "" }

scalaVersion := "2.12.3"

crossScalaVersions := Seq(
  "2.11.8",
  "2.11.11",
  "2.12.0",
  "2.12.1",
  "2.12.2",
  "2.12.3"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1+" % Test
)

scalacOptions ++= {
  if (isSnapshot.value) Seq()
  else scalaVersion.value split '.' map { _.toInt } match {
    case Array(2, 11, _) => Seq("-optimize")
    case Array(2, 12, patch) if patch <= 2 => Seq("-opt:l:project")
    case Array(2, 12, patch) if patch > 2 => Seq("-opt:l:inline")
    case _ => Seq()
  }
}

publishTo := {
  if (isSnapshot.value) Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  else None
}

publishMavenStyle := true
licenses := Seq("The Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/NthPortal/convert"))

pomExtra :=
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

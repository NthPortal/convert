import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val artifactVersion = "0.6.0"
val isArtifactSnapshot = true
lazy val versionSettings = Seq(
  organization := "com.nthportal",
  isSnapshot := isArtifactSnapshot,
  version := artifactVersion + { if (isArtifactSnapshot) "-SNAPSHOT" else "" }
)

val scala211 = "2.11.12"
val scala212 = "2.12.8"
val scala213 = "2.13.0-RC3"

/* global settings */
ThisBuild / scalaVersion := scala212

/* optimisation settings */
lazy val optimisations = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-optimize")
      case Some((2, 12)) => Seq("-opt:l:inline", "-opt-inline-from:com.nthportal.convert.*")
      case _             => Nil
    }
  }
)
lazy val releaseOptimisations = {
  if (isArtifactSnapshot) Nil else optimisations
}

/* shared settings */
lazy val buildSettings = Seq(
  crossScalaVersions := Seq(scala211, scala212, scala213),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.7" % Test
  ),
  autoAPIMappings := true
)
lazy val publishSettings = Seq(
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
lazy val sharedSettings = versionSettings ++ buildSettings ++ publishSettings

/* projects */
lazy val root = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .aggregate(core)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(sharedSettings, releaseOptimisations)
  .settings(
    name := "convert",
    description := "A Scala library for handling conversions between types by throwing exceptions or returning Options " +
      "containing the results."
  )

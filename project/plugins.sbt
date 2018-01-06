logLevel := Level.Warn

addSbtPlugin("org.scala-js"     % "sbt-scalajs"              % "0.6.21")
addSbtPlugin("org.scala-native" % "sbt-crossproject"         % "0.2.2")
addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.2")
//addSbtPlugin("org.scala-native" % "sbt-scala-native"         % "0.3.3")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.2.2")

import AssemblyKeys._

organization := "ru.kulikovd"

name := "gplus-comments"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq(
  "-language:postfixOps",
  "-feature",
  "-deprecation",
  "-Xmigration",
  "-Xcheckinit",
  "-Yinline-warnings",
  "-optimise",
  "-encoding", "utf8"
)

javacOptions ++= Seq("-source", "1.7")

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/groups/public",
  "Spray Nightlies" at "http://nightlies.spray.io/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.1.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.1.2",
  "com.typesafe" % "config" % "1.0.0",
  "ch.qos.logback" % "logback-classic" % "1.0.10" % "runtime",
  "io.spray" % "spray-can" % "1.1-2+",
  "io.spray" % "spray-client" % "1.1-2+",
  "io.spray" % "spray-http" % "1.1-2+",
  "io.spray" % "spray-routing" % "1.1-2+",
  "io.spray" %% "spray-json" % "1.2.3",
  "com.github.bytecask" %% "bytecask" % "1.0-SNAPSHOT"
)

assemblySettings

jarName in assembly := "gplus-comments.jar"

TaskKey[Unit]("upload") := ("scp target/scala-2.10/gplus-comments.jar " + System.getenv("GLUSCOMMENTS_UPLOAD_PATH")).!

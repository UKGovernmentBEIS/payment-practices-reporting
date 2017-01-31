import de.heikoseeberger.sbtheader.CommentStyleMapping._
import de.heikoseeberger.sbtheader.license.GPLv3
import sbtbuildinfo.BuildInfoPlugin.autoImport._

name := "prompt-payments"

startYear := Some(2017)

organization := "uk.gov.beis.digital"

git.useGitDescribe in ThisBuild := true

scalaVersion in ThisBuild := "2.11.8"

enablePlugins(PlayScala)
disablePlugins(PlayLayoutPlugin)
enablePlugins(GitVersioning)
enablePlugins(GitBranchPrompt)
enablePlugins(BuildInfoPlugin)

val SLICK_PG_VERSION = "0.14.3"

val slickpgDependencies = Seq(
  "com.github.tminglei" %% "slick-pg" % SLICK_PG_VERSION,
  "com.github.tminglei" %% "slick-pg_play-json" % SLICK_PG_VERSION,
  "com.github.tminglei" %% "slick-pg_date2" % SLICK_PG_VERSION,
  "com.github.tminglei" %% "slick-pg_joda-time" % SLICK_PG_VERSION
)

libraryDependencies ++= Seq(
  "com.wellfactored" %% "play-bindings" % "2.0.0",
  "com.wellfactored" %% "slick-gen" % "0.0.4",
  "com.github.melrief" %% "pureconfig" % "0.4.0",
  "org.postgresql" % "postgresql" % "9.4.1211",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "joda-time" % "joda-time" % "2.9.6",
  "org.joda" % "joda-convert" % "1.8.1",
  "org.typelevel" %% "cats-core" % "0.9.0",

  "org.scalatest" %% "scalatest" % "3.0.0" % Test)

libraryDependencies ++= slickpgDependencies

PlayKeys.devSettings := Seq("play.server.http.port" -> "9000")

routesImport ++= Seq(
  "com.wellfactored.playbindings.ValueClassUrlBinders._"
)

javaOptions := Seq(
  "-Dconfig.file=src/main/resources/development.application.conf",
  "-Dlogger.file=src/main/resources/development.logger.xml"
)

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "buildinfo"
buildInfoOptions ++= Seq(BuildInfoOption.ToJson, BuildInfoOption.BuildTime)

fork in Test in ThisBuild := true
testForkedParallel in ThisBuild := true

import sbtbuildinfo.BuildInfoPlugin.autoImport._

name := "payment-practices-reporting"

startYear := Some(2017)

organization := "uk.gov.beis.digital"

git.useGitDescribe in ThisBuild := true

scalaVersion in ThisBuild := "2.11.8"

enablePlugins(PlayScala)
disablePlugins(PlayLayoutPlugin)
enablePlugins(GitVersioning)
enablePlugins(GitBranchPrompt)
enablePlugins(BuildInfoPlugin)

resolvers += Resolver.bintrayRepo("gov-uk-notify", "maven")

val playSlickVersion = "2.1.0"

libraryDependencies ++= Seq(
  ws,
  "com.wellfactored" %% "play-bindings" % "2.0.0",
  "com.wellfactored" %% "slick-gen" % "0.0.4",
  "com.github.melrief" %% "pureconfig" % "0.4.0",
  "org.postgresql" % "postgresql" % "9.4.1211",
  "com.typesafe.play" %% "play-slick" % playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion,
  "joda-time" % "joda-time" % "2.9.7",
  "org.joda" % "joda-convert" % "1.8.1",
  "com.github.nscala-time" %% "nscala-time" % "2.16.0",
  "org.typelevel" %% "cats-core" % "0.9.0",
  "com.beachape" %% "enumeratum" % "1.5.2",
  "com.beachape" %% "enumeratum-play-json" % "1.5.2",
  "eu.timepit" %% "refined" % "0.6.1",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "uk.gov.service.notify" % "notifications-java-client" % "3.1.1-RELEASE",

  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test)


PlayKeys.devSettings := Seq("play.server.http.port" -> "9000")

routesImport ++= Seq(
  "com.wellfactored.playbindings.ValueClassUrlBinders._",
  "models._"
)

TwirlKeys.templateImports ++= Seq(
  "views.html.FieldHelpers._",
  "org.joda.time._",
  "org.joda.time.format.DateTimeFormatter"
)

javaOptions := Seq(
  "-Dconfig.file=src/main/resources/development.application.conf",
  "-Dlogger.file=src/main/resources/development.logger.xml"
)

// need this because we've disabled the PlayLayoutPlugin. without it twirl templates won't get
// re-compiled on change in dev mode
PlayKeys.playMonitoredFiles ++= (sourceDirectories in(Compile, TwirlKeys.compileTemplates)).value

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "buildinfo"
buildInfoOptions ++= Seq(BuildInfoOption.ToJson, BuildInfoOption.BuildTime)

fork in Test in ThisBuild := true
testForkedParallel in ThisBuild := true

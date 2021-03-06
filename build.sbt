name := "payment-practices-reporting"

startYear := Some(2017)

organization := "uk.gov.beis.digital"

git.useGitDescribe in ThisBuild := true

scalaVersion in ThisBuild := "2.11.8"

val commonScalacOptions = Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-Xlint",
  "-Xfatal-warnings",
  "-language:higherKinds"
)

scalacOptions ++= commonScalacOptions

lazy val `payment-practices-reporting` = project.in(file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .enablePlugins(GitVersioning)
  .enablePlugins(GitBranchPrompt)
  .configs(IntegrationTest.extend(Test)) // integration tests use utility classes from unit tests
  .settings(Defaults.itSettings: _*)

resolvers += Resolver.bintrayRepo("gov-uk-notify", "maven")
resolvers += Resolver.bintrayRepo("hmrc", "releases")

val playSlickVersion = "2.1.0"

// This is the highest version that supports Play 2.5
val enumeratumVersion = "1.5.11"

val monocleVersion = "1.5.0"
addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  ws,
  "com.wellfactored" %% "play-bindings" % "2.0.0",
  "com.github.melrief" %% "pureconfig" % "0.4.0",
  "org.postgresql" % "postgresql" % "9.4.1211",
  "com.h2database" % "h2" % "1.4.191",
  "com.typesafe.play" %% "play-slick" % playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion,
  "joda-time" % "joda-time" % "2.9.7",
  "org.joda" % "joda-convert" % "1.8.1",
  "com.github.nscala-time" %% "nscala-time" % "2.16.0",

  "org.typelevel" %% "cats-core" % "1.0.1",
  "org.typelevel" %% "cats-effect" % "0.5",

  "com.beachape" %% "enumeratum" % enumeratumVersion,
  "com.beachape" %% "enumeratum-play" % enumeratumVersion,
  "com.beachape" %% "enumeratum-play-json" % enumeratumVersion,

  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,

  "eu.timepit" %% "refined" % "0.6.1",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "uk.gov.service.notify" % "notifications-java-client" % "3.1.1-RELEASE",

  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",

  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test) ++
  serverTestDependencies.map(_ % "it,test")


lazy val serverTestDependencies = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0",
  "com.h2database" % "h2" % "1.4.191"
)


PlayKeys.devSettings := Seq("play.server.http.port" -> "9000")

routesImport ++= Seq(
  "com.wellfactored.playbindings.ValueClassUrlBinders._",
  "questionnaire.Question",
  "controllers.FormPageDefs.MultiPageFormName",
  "controllers.FormPageDefs.ShortFormName",
  "controllers.FormPageDefs.SinglePageFormName",
  "models._"
)

TwirlKeys.templateImports ++= Seq(
  "views.html.FieldHelpers._",
  "org.joda.time._",
  "org.joda.time.format.DateTimeFormatter"
)

val standalone: Boolean = sys.props.contains("STANDALONE")

def playJavaOptions(mode: String): Seq[String] =
  if (standalone) Seq(
    "-Dconfig.file=src/main/resources/standalone.application.conf",
    "-Dlogger.file=src/main/resources/development.logger.xml"
  ) else Seq(
    s"-Dconfig.file=src/main/resources/$mode.application.conf",
    s"-Dlogger.file=src/main/resources/$mode.logger.xml"
  )

javaOptions := playJavaOptions("development")

fork in IntegrationTest := true
javaOptions in IntegrationTest ++= playJavaOptions("it")
parallelExecution in IntegrationTest := false

// need this because we've disabled the PlayLayoutPlugin. without it twirl templates won't get
// re-compiled on change in dev mode
PlayKeys.playMonitoredFiles ++= (sourceDirectories in(Compile, TwirlKeys.compileTemplates)).value

fork in Test in ThisBuild := true
testForkedParallel in ThisBuild := true

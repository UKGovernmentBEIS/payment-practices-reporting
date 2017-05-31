
enablePlugins(DockerPlugin)

maintainer := "Doug Clinton <doug.clinton@digital.bis.gov.uk>"

dockerBaseImage := "openjdk:8u102-jdk"

dockerExposedPorts := Seq(9000)

dockerUpdateLatest := true


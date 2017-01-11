name := "downloader"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.json4s" % "json4s-jackson_2.10" % "3.3.0",
  "org.apache.httpcomponents" % "httpclient" % "4.5",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14",
   "commons-net" % "commons-net" % "3.5"
)

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

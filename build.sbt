import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._

import scalariform.formatter.preferences._

name := "stateful"

val reactiveMongoVersion = "0.11.6"

val akkaV = "2.4.1"

scalaVersion := "2.11.7"

val gitHeadCommitSha = settingKey[String]("current git commit SHA")

val commonScalariform = scalariformSettings :+ (ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
  .setPreference(RewriteArrowSymbols, true))

val commons = Seq(
  organization := "ru.unicorndev",
  scalaVersion := "2.11.7",
  resolvers ++= Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    Resolver.sonatypeRepo("snapshots"),
    Resolver.bintrayRepo("alari", "generic")
  ),
  gitHeadCommitSha in ThisBuild := Process("git rev-parse --short HEAD").lines.head,
  licenses +=("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayPackageLabels := Seq("scala", "play", "api"),
  bintrayRepository := "generic",
  version := "0.1." + gitHeadCommitSha.value
) ++ commonScalariform

commons

lazy val `stateful` = (project in file(".")).settings(commons: _*).settings(
  version := "0.2.7",
  libraryDependencies ++= Seq(
    "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "1.1.10",
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaV,
    "joda-time" % "joda-time" % "2.8.1" % Provided,
    "org.joda" % "joda-convert" % "1.7" % Provided,
    "org.slf4j" % "slf4j-simple" % "1.7.12" % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % Test,
    "org.scalatest" %% "scalatest" % "2.2.5" % Test,
    "junit" % "junit" % "4.12" % Test,
    "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.50.1" % Test
  )
)

offline := true

//resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.jcenterRepo

parallelExecution in Test := false

fork in Test := true

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}

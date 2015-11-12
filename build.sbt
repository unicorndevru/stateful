import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._

import scalariform.formatter.preferences._

name := "stateful-gather"

version := "0.2.0"

val reactiveMongoVersion = "0.11.7.play24"

val akkaV = "2.4.0"

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
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayPackageLabels := Seq("scala", "play", "api"),
  bintrayRepository := "generic"
) ++ commonScalariform

commons

lazy val `stateful-gather` = (project in file("."))
  .dependsOn(`stateful`)
  .aggregate(`stateful`)

lazy val `eventbus` = (project in file("eventbus")).settings(commons: _*).settings(
  version := "0.2.1",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV % Provided,
    "com.google.inject" % "guice" % "4.0" % Provided,
    "org.specs2" %% "specs2-core" % "3.6" % Test
  )
)

lazy val `stateful` = (project in file("stateful")).settings(commons: _*).settings(
  version := "0.2.7",
  libraryDependencies ++= Seq(
    "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "1.0.6",
    "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaV,
    "com.google.inject" % "guice" % "4.0" % Provided,
    "joda-time" % "joda-time" % "2.8.1" % Provided,
    "org.joda" % "joda-convert" % "1.7" % Provided
  )
)
  .dependsOn(`eventbus`)
  .aggregate(`eventbus`)

offline := true

//resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.jcenterRepo

testOptions in Test += Tests.Argument("junitxml")

parallelExecution in Test := false

fork in Test := true

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}

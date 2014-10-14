import com.typesafe.sbt.SbtNativePackager.Universal

import com.typesafe.sbt.packager.Keys._

import scalariform.formatter.preferences._

packageArchetype.java_application

organization := "com.agourlay"

name := "omnibus"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions := Seq(
  "-unchecked",
  "-Xlint",
  "-deprecation",
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-Ywarn-dead-code",
  "-language:_",
  "-feature"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(AlignParameters, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)

mappings in Universal += {
  file("src/main/resources/application.conf") -> "conf/omnibus.conf"
}

scriptClasspath += "../conf/omnibus.conf"

resolvers ++= Seq(
   "sonatype releases"  at "https://oss.sonatype.org/content/repositories/releases/"
  ,"typesafe release"   at "http://repo.typesafe.com/typesafe/releases/"
  ,"typesafe repo"      at "http://repo.typesafe.com/typesafe/repo/"
  ,"maven central"      at "http://repo1.maven.org/maven2/"
  ,"akka repo"          at "http://repo.akka.io/"
  ,"spray repo"         at "http://repo.spray.io/"
)

val test = project.in(file("."))
  .enablePlugins(GatlingPlugin)
  .settings(libraryDependencies ++= {
    val gatlingV     = "2.0.1"
    val gatlingTestV = "1.0"
    Seq(
       "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV     % "it"
      ,"io.gatling"            % "test-framework"            % gatlingTestV % "it"
    )
  })


libraryDependencies ++= {
  val akkaV         = "2.3.6"
  val sprayV        = "1.3.2"
  val sprayJsonV    = "1.3.0"
  val sprayWsV      = "0.1.3"
  val logbackV      = "1.1.2"
  val scalaTestV    = "2.2.2"
  val scalaMetricsV = "3.3.0_a2.3"
  val metricsV      = "3.1.0"
  val levelDbV      = "0.7"
  Seq(
     "io.spray"              %% "spray-can"                     % sprayV                      withSources() 
    ,"io.spray"              %% "spray-routing"                 % sprayV                      withSources()
    ,"io.spray"              %% "spray-caching"                 % sprayV                      withSources()
    ,"io.spray"              %% "spray-json"                    % sprayJsonV                  withSources()
    ,"com.wandoulabs.akka"   %% "spray-websocket"               % sprayWsV                    withSources()
    ,"com.typesafe.akka"     %% "akka-actor"                    % akkaV                       withSources()
    ,"com.typesafe.akka"     %% "akka-slf4j"                    % akkaV                       withSources()
    ,"com.typesafe.akka"     %% "akka-persistence-experimental" % akkaV                       withSources()
    ,"org.iq80.leveldb"      %  "leveldb"                       % levelDbV                    withSources()
    ,"nl.grons"              %% "metrics-scala"                 % scalaMetricsV               withSources()
    ,"io.dropwizard.metrics" %  "metrics-graphite"              % metricsV                    withSources()
    ,"ch.qos.logback"        %  "logback-classic"               % logbackV                    withSources()
    ,"io.spray"              %% "spray-testkit"                 % sprayV         % "test"     withSources()
    ,"com.typesafe.akka"     %% "akka-testkit"                  % akkaV          % "test"     withSources()
    ,"org.scalatest"         %% "scalatest"                     % scalaTestV     % "test"     withSources()
  )
}

seq(Revolver.settings: _*)

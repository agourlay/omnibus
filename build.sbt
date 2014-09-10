import com.typesafe.sbt.SbtNativePackager.Universal

import com.typesafe.sbt.packager.Keys._

packageArchetype.java_application

organization := "com.agourlay"

name := "omnibus"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions := Seq(
  "-unchecked",
  "-Xlint",
  "-deprecation",
  "-encoding","utf8",
  "-Ywarn-dead-code",
  "-language:_",
  "-feature")

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
  ,"akka cassandra"     at "http://dl.bintray.com/krasserm/maven"
)

val test = project.in(file("."))
  .enablePlugins(GatlingPlugin)
  .settings(libraryDependencies ++= {
    val gatlingV     = "2.0.0-RC4"
    val gatlingTestV = "1.0-RC4"
    Seq(
       "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV     % "test"
      ,"io.gatling"            % "test-framework"            % gatlingTestV % "test"
    )
  })


libraryDependencies ++= {
  val akkaV         = "2.3.6"
  val sprayV        = "1.3.2-20140909"
  val sprayJsonV    = "1.2.6"
  val sprayWsV      = "0.1.3"
  val logbackV      = "1.1.2"
  val specs2V       = "2.4.2"
  val scalaCheckV   = "1.11.5"
  val scalaTestV    = "2.2.2"
  val akkaCassanV   = "0.3.3"
  val scalaMetricsV = "3.2.1_a2.3"
  val metricsV      = "3.0.2"
  Seq(
     "io.spray"             %% "spray-can"                     % sprayV               withSources() 
    ,"io.spray"             %% "spray-routing"                 % sprayV               withSources()
    ,"io.spray"             %% "spray-caching"                 % sprayV               withSources()
    ,"io.spray"             %% "spray-json"                    % sprayJsonV           withSources()
    ,"com.wandoulabs.akka"  %% "spray-websocket"               % sprayWsV             withSources()
    ,"com.typesafe.akka"    %% "akka-actor"                    % akkaV                withSources()
    ,"com.typesafe.akka"    %% "akka-slf4j"                    % akkaV                withSources()
    ,"com.typesafe.akka"    %% "akka-persistence-experimental" % akkaV                withSources()
    ,"com.github.krasserm"  %% "akka-persistence-cassandra"    % akkaCassanV          withSources()
    ,"nl.grons"             %% "metrics-scala"                 % scalaMetricsV        withSources()
    ,"com.codahale.metrics" %  "metrics-graphite"              % metricsV             withSources()
    ,"ch.qos.logback"       %  "logback-classic"               % logbackV             withSources()
    ,"io.spray"             %% "spray-testkit"                 % sprayV      % "test" withSources()
    ,"com.typesafe.akka"    %% "akka-testkit"                  % akkaV       % "test" withSources()
    ,"org.specs2"           %% "specs2-core"                   % specs2V     % "test" withSources()
    ,"org.scalacheck"       %% "scalacheck"                    % scalaCheckV % "test" withSources()
    ,"org.scalatest"        %% "scalatest"                     % scalaTestV  % "test" withSources()
  )
}

seq(Revolver.settings: _*)

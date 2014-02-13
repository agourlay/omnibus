import com.typesafe.sbt.SbtNativePackager.Universal

import com.typesafe.sbt.packager.Keys._

net.virtualvoid.sbt.graph.Plugin.graphSettings

packageArchetype.java_application

organization := "com.agourlay"

name := "omnibus"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4-RC3"

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
  ,"sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  ,"typesafe release"   at "http://repo.typesafe.com/typesafe/releases/"
  ,"typesafe repo"      at "http://repo.typesafe.com/typesafe/repo/"
  ,"typesafe snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
  ,"maven central"      at "http://repo1.maven.org/maven2/"
  ,"akka repo"          at "http://repo.akka.io/"
  ,"akka snapshots"     at "http://repo.akka.io/snapshots"
  ,"spray repo"         at "http://repo.spray.io/"
  ,"akka cassandra"     at "http://dl.bintray.com/krasserm/maven"
)

libraryDependencies ++= {
  val akkaV       = "2.3.0-RC2"
  val sprayV      = "1.3-RC1"
  val sprayJsonV  = "1.2.5"
  val logbackV    = "1.1.1"
  val specs2V     = "2.3.8"
  val scalaCheckV = "1.11.3"
  val scalaTestV  = "2.1.0-RC2"
  val akkaCassanV = "0.1"
  val gatlingV    = "2.0.0-SNAPSHOT"
  Seq(
       "io.spray"              %   "spray-can"                      % sprayV                  withSources() 
      ,"io.spray"              %   "spray-routing"                  % sprayV                  withSources()
      ,"io.spray"              %   "spray-caching"                  % sprayV                  withSources()
      ,"io.spray"              %%  "spray-json"                     % sprayJsonV              withSources()
      ,"com.typesafe.akka"     %%  "akka-actor"                     % akkaV                   withSources()
      ,"com.typesafe.akka"     %%  "akka-slf4j"                     % akkaV                   withSources()
      ,"com.typesafe.akka"     %%  "akka-persistence-experimental"  % akkaV                   withSources()
      ,"com.github.krasserm"   %%  "akka-persistence-cassandra"     % akkaCassanV             withSources()
      ,"ch.qos.logback"        %   "logback-classic"                % logbackV                withSources()
      ,"io.spray"              %   "spray-testkit"                  % sprayV       % "test"   withSources()
      ,"com.typesafe.akka"     %%  "akka-testkit"                   % akkaV        % "test"   withSources()
      ,"org.specs2"            %%  "specs2-core"                    % specs2V      % "test"   withSources()
      ,"org.scalacheck"        %%  "scalacheck"                     % scalaCheckV  % "test"   withSources()
      ,"org.scalatest"         %%  "scalatest"                      % scalaTestV   % "test"   withSources()
      ,"io.gatling.highcharts" %  "gatling-charts-highcharts"       % gatlingV     % "test"   withSources()
  )
}

seq(Revolver.settings: _*)

import scalariform.formatter.preferences._

organization := "com.agourlay"

name := "omnibus"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-Ywarn-dead-code",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ywarn-unused-import",
  "-feature"
)

fork in Test := true

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(AlignParameters, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)

enablePlugins(JavaServerAppPackaging)

mappings in Universal += {
  file("src/main/resources/application.conf") -> "conf/omnibus.conf"
}

scriptClasspath += "../conf/omnibus.conf"

resolvers ++= Seq(
  "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"
  ,"spray"        at "http://repo.spray.io"
)

val test = project.in(file("."))
  .enablePlugins(GatlingPlugin)
  .settings(libraryDependencies ++= {
    val gatlingV  = "2.1.7"
    val commonIoV = "2.4"
    Seq(
       "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV  % "it"
      ,"io.gatling"            % "gatling-test-framework"    % gatlingV  % "it"
      ,"commons-io"            % "commons-io"                % commonIoV % "it"
    )
  })

libraryDependencies ++= {
  val akkaV         = "2.3.12"
  val sprayV        = "1.3.3"
  val sprayJsonV    = "1.3.2"
  val sprayWsV      = "0.1.4"
  val logbackV      = "1.1.3"
  val scalaTestV    = "2.2.5"
  val scalaMetricsV = "3.5.1_a2.3"
  val metricsV      = "3.1.2"
  val levelDbV      = "1.8"
  Seq(
     "io.spray"                  %% "spray-can"                     % sprayV
    ,"io.spray"                  %% "spray-routing-shapeless2"      % sprayV
    ,"io.spray"                  %% "spray-json"                    % sprayJsonV
    ,"com.wandoulabs.akka"       %% "spray-websocket"               % sprayWsV
    ,"com.typesafe.akka"         %% "akka-actor"                    % akkaV
    ,"com.typesafe.akka"         %% "akka-slf4j"                    % akkaV
    ,"com.typesafe.akka"         %% "akka-persistence-experimental" % akkaV
    ,"org.fusesource.leveldbjni" %  "leveldbjni-all"                % levelDbV
    ,"nl.grons"                  %% "metrics-scala"                 % scalaMetricsV
    ,"io.dropwizard.metrics"     %  "metrics-graphite"              % metricsV
    ,"ch.qos.logback"            %  "logback-classic"               % logbackV
    ,"io.spray"                  %% "spray-testkit"                 % sprayV         % "test"
    ,"com.typesafe.akka"         %% "akka-testkit"                  % akkaV          % "test"
    ,"org.scalatest"             %% "scalatest"                     % scalaTestV     % "test"
  )
}

Seq(Revolver.settings: _*)

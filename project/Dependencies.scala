import sbt._

object Dependencies {
  val catsEffectVersion      = "1.2.0"
  val scalaTestVersion       = "3.0.5"
  val sttpCatsBackendVersion = "1.5.15"
  val sttpVersion            = "1.5.15"
  val sl4jVersion            = "1.7.26"
  val http4sVersion          = "0.20.0"
  val pureConfigVersion      = "0.10.2"
  val jaegerClientVersion    = "0.34.0"
  val opentracingMockVersion = "0.32.0"

  lazy val appDeps = Seq(
    "com.softwaremill.sttp" %% "core"                           % sttpVersion,
    "org.slf4j"             % "slf4j-simple"                    % sl4jVersion,
    "org.http4s"            %% "http4s-dsl"                     % http4sVersion,
    "org.http4s"            %% "http4s-blaze-server"            % http4sVersion,
    "org.typelevel"         %% "cats-effect"                    % catsEffectVersion,
    "com.github.pureconfig" %% "pureconfig"                     % pureConfigVersion,
    "io.jaegertracing"      % "jaeger-client"                   % jaegerClientVersion,
    "com.softwaremill.sttp" %% "async-http-client-backend-cats" % sttpCatsBackendVersion
  ).map(_ withSources () withJavadoc ())

  lazy val testDeps = Seq(
    "org.scalatest"  %% "scalatest"       % scalaTestVersion,
    "io.opentracing" % "opentracing-mock" % opentracingMockVersion
  ).map(_ % Test withSources () withJavadoc ())
}

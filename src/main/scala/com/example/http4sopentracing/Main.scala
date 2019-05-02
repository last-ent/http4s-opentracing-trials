package com.example.http4sopentracing

import cats.effect._
import cats.implicits._
import com.example.http4sopentracing.middleware.TraceMiddleware
import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration}
import io.opentracing.Tracer
import org.http4s.implicits._
import cats.data.Kleisli
import org.http4s.{Request, Response}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

import com.example.http4sopentracing.client.HttpClient
import com.example.http4sopentracing.health.HealthRoute
import com.example.http4sopentracing.entrypoint.EntryPoint
import com.example.http4sopentracing.servicea.ServiceA
import com.example.http4sopentracing.serviceb.ServiceB
import com.example.http4sopentracing.servicec.ServiceC
import com.example.http4sopentracing.serviceiam.ServiceIam

case class JaegerConfig(
    serviceName:      String,
    samplerType:      String,
    samplerParam:     Int,
    reporterLogSpans: Boolean
)

object Server extends IOApp {
  val httpClient = new HttpClient()

  val services = List(
    new ServiceB(httpClient).service,
    new ServiceC(httpClient).service,
    new ServiceIam(httpClient).service
  ).foldLeft(HealthRoute.healthCheck)(_ <+> _) // (acc, item)

  val tracer = getTracer

  val httpApp: Kleisli[IO, Request[IO], Response[IO]] = Router(
    "/" -> services,
    "/entrypoint" -> TraceMiddleware.wrap(httpClient, "entrypoint", tracer, EntryPoint.service),
    "/servicea" -> TraceMiddleware.wrap(httpClient, "service-a", tracer, ServiceA.service)
  ).orNotFound

  def getTracer: Tracer = {
    val jConfig = JaegerConfig(
      "server-trace",
      "const",
      1,
      true
    )
    val config         = Configuration.fromEnv(jConfig.serviceName)
    val samplerConfig  = SamplerConfiguration.fromEnv().withType(jConfig.samplerType).withParam(jConfig.samplerParam)
    val reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(jConfig.reporterLogSpans)
    config.withSampler(samplerConfig).withReporter(reporterConfig).getTracer
  }

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}

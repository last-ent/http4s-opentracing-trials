package com.example.http4sopentracing

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import com.example.http4sopentracing.client.HttpClient
import com.example.http4sopentracing.entrypoint.EntryPoint
import com.example.http4sopentracing.health.HealthRoute
import com.example.http4sopentracing.middleware.TraceMiddleware
import com.example.http4sopentracing.servicea.ServiceA
import com.example.http4sopentracing.serviceb.ServiceB
import com.example.http4sopentracing.servicec.ServiceC
import com.example.http4sopentracing.serviceiam.ServiceIam
import com.example.http4sopentracing.finale.FinalRoute
import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration}
import io.opentracing.Tracer
import org.http4s.{Request, Response}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

case class JaegerConfig(
    serviceName:      String,
    samplerType:      String,
    samplerParam:     Int,
    reporterLogSpans: Boolean
)

object Server extends IOApp {
  val httpClient = new HttpClient()

  val tracer = getTracer

  val middlewarePA = TraceMiddleware.wrap(httpClient, tracer)(_, _)

  val httpApp: Kleisli[IO, Request[IO], Response[IO]] = Router(
    "/health" -> HealthRoute.healthCheck,
    "/entrypoint" -> middlewarePA("entrypoint", EntryPoint.service),
    "/servicea" -> middlewarePA("service-a", ServiceA.service),
    "/serviceb" -> middlewarePA("service-b", ServiceB.service),
    "/servicec" -> middlewarePA("service-c", ServiceC.service),
    "/serviceiam" -> middlewarePA("service-iam", ServiceIam.service),
    "/final" -> middlewarePA("final", FinalRoute.service)
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

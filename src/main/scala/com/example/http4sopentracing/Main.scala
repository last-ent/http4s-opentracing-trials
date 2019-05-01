package com.example.http4sopentracing

import cats.effect._
import cats.implicits._
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
import com.example.http4sopentracing.tracing.config.{JaegerConfig, ReporterConfig, SamplerConfig, SamplerType}

object Server extends IOApp {
  val httpClient = new HttpClient()

  val jaegerConfig = JaegerConfig(
    "http4s-opentracing",
    SamplerConfig(SamplerType.ConstSamplerType, 1),
    ReporterConfig(true)
  )

  val services = List(
    new EntryPoint(httpClient).service,
    new ServiceA(httpClient).service,
    new ServiceB(httpClient).service,
    new ServiceC(httpClient).service,
    new ServiceIam(httpClient).service
  ).foldLeft(HealthRoute.healthCheck)(_ <+> _) // (acc, item)

  val httpApp: Kleisli[IO, Request[IO], Response[IO]] = Router("/" -> services).orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}

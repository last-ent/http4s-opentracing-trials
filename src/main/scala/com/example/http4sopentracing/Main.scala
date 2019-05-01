package com.example.http4sopentracing

import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import cats.data.Kleisli
import org.http4s.{Request, Response}
import org.http4s.server.Router
import org.log4s.{getLogger, Logger}
import org.http4s.server.blaze.BlazeServerBuilder
import com.example.http4sopentracing.health.HealthRoute

object Server extends IOApp {
  val logger: Logger = getLogger("simple log")

  val services = HealthRoute.healthCheck

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

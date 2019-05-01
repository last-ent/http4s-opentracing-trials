package com.example.http4sopentracing.servicec

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import org.http4s._
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}

class ServiceC(httpClient: HttpClient) {
  val logger: Logger = getLogger("ServiceC")

  val service = HttpRoutes.of[IO] {
    case GET -> Root / "servicec" =>
      logger.info("Received request")
      Ok()
  }
}

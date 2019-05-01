package com.example.http4sopentracing.serviceiam

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import org.http4s._
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}

class ServiceIam(httpClient: HttpClient) {
  val logger: Logger = getLogger("ServiceIam")

  val service = HttpRoutes.of[IO] {
    case GET -> Root / "serviceiam" =>
      logger.info("Received request")
      Ok()
  }
}

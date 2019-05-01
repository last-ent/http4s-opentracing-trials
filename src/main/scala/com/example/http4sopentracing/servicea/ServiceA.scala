package com.example.http4sopentracing.servicea

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import org.http4s._
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}

class ServiceA(httpClient: HttpClient) {
  val logger: Logger = getLogger("ServiceA")

  val service = HttpRoutes.of[IO] {
    case GET -> Root / "servicea" =>
      logger.info("Received request")
      Ok()
  }
}

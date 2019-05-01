package com.example.http4sopentracing.serviceb

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import org.http4s._
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}

class ServiceB(httpClient: HttpClient) {
  val logger: Logger = getLogger("ServiceB")

  val service = HttpRoutes.of[IO] {
    case GET -> Root / "serviceb" =>
      logger.info("Received request")
      Ok()
  }
}

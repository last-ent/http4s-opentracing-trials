package com.example.http4sopentracing.entrypoint

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import org.http4s.{HttpRoutes, Response => HttpResponse}
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}
import com.softwaremill.sttp._
import cats.implicits._
import scala.concurrent.ExecutionContext.Implicits.global

class EntryPoint(httpClient: HttpClient) {
  implicit val contextShift = IO.contextShift(global)

  val logger: Logger = getLogger("EntryPoint")

  val baseUrl = "http://127.0.0.1:8080"

  val service = HttpRoutes.of[IO] {
    case GET -> Root / "entrypoint" =>
      for {
        _ <- IO.delay(logger.info("Received request"))
        _ <- httpClient.get(uri"$baseUrl/serviceiam")
        abc <- (
          httpClient.get(uri"$baseUrl/servicea"),
          httpClient.get(uri"$baseUrl/serviceb"),
          httpClient.get(uri"$baseUrl/servicec")
        ).parMapN((_, _, _))
        _ <- IO.delay(logger.info(abc.toString))
      } yield HttpResponse(Ok)
  }
}

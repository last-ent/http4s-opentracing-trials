package com.example.http4sopentracing.entrypoint

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import com.example.http4sopentracing.tracer.TraceContext
import org.http4s.{HttpRoutes, Response => HttpResponse}
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}
import com.softwaremill.sttp._
import cats.implicits._
import scala.concurrent.ExecutionContext.Implicits.global

object EntryPoint {
  implicit val contextShift = IO.contextShift(global)

  val logger: Logger = getLogger("EntryPoint")

  val baseUrl = "http://127.0.0.1:8080"

  def service(httpClient: HttpClient, traceCtx: TraceContext) = HttpRoutes.of[IO] {
    case req @ GET -> Root =>
      for {
        _ <- IO.delay(logger.info("Received request"))
        _ <- traceCtx.withChildSpan("call_iam").use { _ =>
          httpClient.get(uri"$baseUrl/serviceiam")
        }
        abc <- (
          traceCtx.withChildSpan("call_servicea").use { _ =>
            httpClient.get(uri"$baseUrl/servicea")
          },
          traceCtx.withChildSpan("call_serviceb").use { _ =>
            httpClient.get(uri"$baseUrl/serviceb")
          },
          traceCtx.withChildSpan("call_servicec").use { _ =>
            httpClient.get(uri"$baseUrl/servicec")
          }
        ).parMapN((_, _, _))
        _ <- IO.delay(logger.info(abc.toString))
      } yield HttpResponse(Ok)
  }
}

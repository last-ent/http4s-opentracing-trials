package com.example.http4sopentracing.servicea

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import com.example.http4sopentracing.tracer.TraceContext
import org.http4s._
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}

import org.http4s.{HttpRoutes, Response => HttpResponse}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ServiceA {
  implicit val timer        = IO.timer(global)
  implicit val contextShift = IO.contextShift(global)
  val logger: Logger        = getLogger("ServiceA")

  def service(httpClient: HttpClient, traceCtx: TraceContext) = HttpRoutes.of[IO] {
    case GET -> Root =>
      for {
        _ <- IO.delay(logger.info("Received request"))
        _ <- traceCtx.withSpan("call_db").use { _ =>
          IO.sleep(2.seconds)
        }
        _ <- IO.delay(logger.info("slept"))
      } yield HttpResponse(Ok)
  }
}

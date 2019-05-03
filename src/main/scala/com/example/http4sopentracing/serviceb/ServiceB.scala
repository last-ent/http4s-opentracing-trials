package com.example.http4sopentracing.serviceb

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import com.example.http4sopentracing.tracer.TraceContext
import org.http4s.{HttpRoutes, Response => HttpResponse}
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ServiceB {
  val logger: Logger        = getLogger("ServiceB")
  implicit val timer        = IO.timer(global)
  implicit val contextShift = IO.contextShift(global)

  def service(httpClient: HttpClient, traceCtx: TraceContext) = HttpRoutes.of[IO] {
    case GET -> Root =>
      for {
        _ <- IO.delay(logger.info("Received request"))
        _ <- traceCtx.withChildSpan("call_final").use { _ =>
          IO.sleep(50.milliseconds)
        }
      } yield HttpResponse(Ok)
  }
}

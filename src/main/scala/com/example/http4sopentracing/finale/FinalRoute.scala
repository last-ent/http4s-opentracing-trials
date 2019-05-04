package com.example.http4sopentracing.finale

import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import com.example.http4sopentracing.tracer.TraceContext
import org.http4s._
import org.http4s.dsl.io._
import org.log4s.{getLogger, Logger}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object FinalRoute {
  val logger: Logger        = getLogger("FinalRoute")
  implicit val timer        = IO.timer(global)
  implicit val contextShift = IO.contextShift(global)

  def service(httpClient: HttpClient, traceCtx: TraceContext) = HttpRoutes.of[IO] {
    case GET -> Root =>
      for {
        _ <- IO.delay(logger.info("Received request"))
        _ <- IO.sleep(50.milliseconds)
      } yield Response(Ok)
  }
}

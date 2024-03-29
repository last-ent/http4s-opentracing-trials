package com.example.http4sopentracing.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import com.example.http4sopentracing.tracer.TraceContext
import io.opentracing.Tracer
import org.http4s.{HttpRoutes, Request}

object TraceMiddleware {
  def wrap(httpClient: HttpClient, tracer: Tracer)(
      rootSpanName:    String,
      service:         (HttpClient, TraceContext) => HttpRoutes[IO]): HttpRoutes[IO] =
    Kleisli { req: Request[IO] =>
      OptionT {
        for {
          traceCtx <- TraceContext.extractFrom(req.headers, rootSpanName, tracer)
          resp <- traceCtx.withCurrentSpan().use { _ =>
            service(httpClient, traceCtx).run(req).value
          }
        } yield resp
      }
    }
}

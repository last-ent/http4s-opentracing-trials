package com.example.http4sopentracing.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import com.example.http4sopentracing.client.HttpClient
import io.opentracing.Tracer
import com.example.http4sopentracing.tracer.{FlowId, TraceContext}
import org.http4s.{HttpRoutes, Request}

object TraceMiddleware {
  def wrap(
      httpClient:   HttpClient,
      rootSpanName: String,
      tracer:       Tracer,
      service:      (HttpClient, TraceContext) => HttpRoutes[IO]): HttpRoutes[IO] =
    Kleisli { req: Request[IO] =>
      OptionT {
        for {
          traceCtx <- TraceContext(rootSpanName, FlowId.from(None), tracer)
          resp     <- traceCtx.contain().use(_ => service(httpClient, traceCtx).run(req).value)
        } yield resp
      }
    }
}

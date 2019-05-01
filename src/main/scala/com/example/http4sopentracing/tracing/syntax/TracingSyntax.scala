package com.example.http4sopentracing.tracing.syntax

import cats.effect.IO
import com.example.http4sopentracing.tracing.config.TraceContext

trait TracingSyntax {
  implicit final def tracingSyntax[A](effect: IO[A]): TracingSyntaxOps[A] = new TracingSyntaxOps(effect)
}

class TracingSyntaxOps[A](effect: IO[A]) {
  def withinTrace(spanName: String, traceCtx: TraceContext): IO[A] =
    for {
      newSpan <- IO.delay(traceCtx.localTracer.createChildSpan(spanName, traceCtx.span))
      result  <- effect
      _       <- IO.delay(traceCtx.localTracer.closeSpan(newSpan))
    } yield result

}

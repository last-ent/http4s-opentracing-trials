package com.example.http4sopentracing.tracer

import cats.effect.{IO, Resource}
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.{Span, Tracer}

case class TraceContext(span: Span, flowId: FlowId, tracer: Tracer) {
  def withSpan(spanName: String, tags: List[(String, String)] = List.empty): Resource[IO, TraceContext] =
    Resource.make {
      for {
        spanBuilder <- TraceContext.buildSpan(spanName, ("flow_id", flowId.value) :: tags, tracer)
        span        <- IO.delay(spanBuilder.asChildOf(span).start())
      } yield this.copy(span = span)
    } { traceCtx =>
      closeSpan(traceCtx.span)
    }

  def closeSpan(span: Span): IO[Unit] = IO.delay(span.finish())

  def contain(tags: List[(String, String)] = List.empty): Resource[IO, List[(String, String)]] =
    Resource.make {
      IO.pure(tags)
    } { _ =>
      closeSpan(span)
    }
}

object TraceContext {
  def apply(spanName: String, flowId: FlowId, tracer: Tracer): IO[TraceContext] =
    for {
      spanBuilder <- buildSpan(spanName, List.empty, tracer)
      span        <- IO.delay(spanBuilder.start())
    } yield TraceContext(span, flowId, tracer)

  def apply(tracer: Tracer)(spanName: String, flowId: FlowId): IO[TraceContext] =
    TraceContext(spanName, flowId, tracer)

  def buildSpan(spanName: String, tags: List[(String, String)], tracer: Tracer): IO[SpanBuilder] =
    for {
      spanBuilder <- IO.delay(tracer.buildSpan(spanName))
      taggedSB    <- tagSpan(spanBuilder, tags)
    } yield taggedSB

  def tagSpan(spanBuilder: SpanBuilder, tags: List[(String, String)]): IO[SpanBuilder] =
    IO.delay(tags.foldLeft(spanBuilder) {
      case (accSpan, (tag, tagValue)) => accSpan.withTag(tag, tagValue)
    })
}

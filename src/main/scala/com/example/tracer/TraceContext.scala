package com.example.http4sopentracing.tracer

import cats.effect.{IO, Resource}
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.tag.Tags
import io.opentracing.{Span, Tracer}
import scala.collection.JavaConverters._
import cats.implicits._

case class TraceContext(span: Span, flowId: FlowId, tracer: Tracer, tags: List[(String, String)] = List.empty) {
  def withChildSpan(spanName: String, tags: List[(String, String)] = List.empty): Resource[IO, TraceContext] =
    Resource.make {
      for {
        spanBuilder <- TraceContext.buildSpan(spanName, ("flow_id", flowId.value) :: tags, tracer)
        span        <- IO.delay(spanBuilder.asChildOf(span).start())
      } yield this.copy(span = span, tags = tags)
    } { traceCtx =>
      closeSpan(traceCtx.span)
    }

  def closeSpan(span: Span): IO[Unit] = IO.delay(span.finish())

  def withCurrentSpan(tags: List[(String, String)] = List.empty): Resource[IO, TraceContext] =
    Resource.make {
      IO.pure(this.copy(tags = tags))
    } { _ =>
      closeSpan(span)
    }

  def logToSpan(msg: String): IO[TraceContext] = IO.delay(span.log(msg)) >> IO.delay(this) // FlatMap while ignoring return value

  def logErrorToSpan(err: RuntimeException): IO[Unit] =
    toSpanError(err).flatMap { errorMap =>
      IO.delay(
        span
          .setTag(Tags.ERROR.getKey, true)
          .log(errorMap.asJava)
      )
    }

  private def toSpanError(err: RuntimeException): IO[Map[String, String]] =
    IO.delay(
      Map(
        "error.kind" -> Option(err.getClass.getName).getOrElse("class-name-not-found"),
        "event" -> "error",
        "message" -> Option(err.getMessage).getOrElse("exception-with-empty-message"),
        "stack" -> Some(err.getStackTrace.mkString("\n")).getOrElse("no-stacktrace-available")
      )
    )
}

object TraceContext {
  def apply(spanName: String, flowId: FlowId, tracer: Tracer): IO[TraceContext] =
    for {
      spanBuilder <- buildSpan(spanName, List.empty, tracer)
      span        <- IO.delay(spanBuilder.start())
    } yield TraceContext(span, flowId, tracer)

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

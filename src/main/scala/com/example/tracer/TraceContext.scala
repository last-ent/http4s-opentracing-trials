package com.example.http4sopentracing.tracer

import cats.effect.{IO, Resource}
import io.opentracing.SpanContext
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.propagation.{Format, TextMapExtractAdapter}
import io.opentracing.tag.Tags
import io.opentracing.{Span, Tracer}
import org.http4s.util.CaseInsensitiveString
import scala.collection.JavaConverters._
import cats.implicits._
import scala.util.{Failure, Success, Try}

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
  def apply(spanName: String, flowId: FlowId, tracer: Tracer): IO[TraceContext] = create(spanName, flowId, tracer)

  def create(spanName: String, flowId: FlowId, tracer: Tracer): IO[TraceContext] =
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

  def extractFrom(headers: org.http4s.Headers, rootSpanName: String, tracer: Tracer): IO[TraceContext] =
    for {
      adapter     <- createTMAdapterFrom(headers)
      spanContext <- trySpanCtxExtraction(adapter, tracer)
      flowIdOpt   <- IO.pure(headers.get(CaseInsensitiveString("flow_id")).map(_.value))
      newTraceCtx <- create(rootSpanName, FlowId.from(flowIdOpt), tracer)
    } yield newTraceCtx

  private def createTMAdapterFrom(headers: org.http4s.Headers) =
    IO.delay(
      new TextMapExtractAdapter(
        headers.toList.map { header =>
          header.name.toString() -> header.value.toString()
        }.toMap.asJava
      )
    )

  private def trySpanCtxExtraction(adapter: TextMapExtractAdapter, tracer: Tracer): IO[SpanContext] =
    Try(tracer.extract(Format.Builtin.HTTP_HEADERS, adapter)) match {
      case Success(spanContext) => IO.pure(spanContext)
      case Failure(exception)   => IO.raiseError(new RuntimeException(s"Error extracting span context: ${exception.getMessage}"))
    }
}

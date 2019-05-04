package com.example.http4sopentracing.tracer

import cats.effect.{IO, Resource}
import io.opentracing.SpanContext
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.propagation.{Format, TextMapExtractAdapter, TextMapInjectAdapter}
import io.opentracing.tag.Tags
import io.opentracing.{Span, Tracer}
import org.http4s.util.CaseInsensitiveString
import scala.collection.JavaConverters._
import cats.implicits._
import scala.util.{Failure, Success, Try}
import org.log4s.getLogger

case class TraceContext(span: Span, flowId: FlowId, tracer: Tracer, tags: List[(String, String)] = List.empty) {
  def withChildSpan(spanName: String, newTags: List[(String, String)] = List.empty): Resource[IO, TraceContext] =
    Resource.make {
      for {
        spanTags    <- IO.delay(("flow_id", flowId.value) :: newTags ++ tags)
        spanBuilder <- TraceContext.buildSpan(spanName, spanTags, tracer)
        span        <- IO.delay(spanBuilder.asChildOf(span).start())
      } yield this.copy(span = span, tags = spanTags)
    } { traceCtx =>
      closeSpan(traceCtx.span)
    }

  def closeSpan(span: Span): IO[Unit] = IO.delay(span.finish())

  def withCurrentSpan(newTags: List[(String, String)] = List.empty): Resource[IO, TraceContext] =
    Resource.make {
      IO.pure(this.copy(tags = tags ++ newTags))
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

  def getHttpHeaders(): Map[String, String] = {
    val headerMap: java.util.Map[String, String] = new java.util.HashMap[String, String]

    tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new TextMapInjectAdapter(headerMap))
    headerMap.asScala.toMap
  }

}

object TraceContext {
  val logger = getLogger("TraceContext")

  def apply(spanName: String, flowId: FlowId, tracer: Tracer, tags: List[(String, String)]): IO[TraceContext] =
    create(spanName, flowId, tracer, tags)

  def create(spanName: String, flowId: FlowId, tracer: Tracer, tags: List[(String, String)]): IO[TraceContext] =
    for {
      spanBuilder <- buildSpan(spanName, tags, tracer)
      span        <- IO.delay(spanBuilder.start())
    } yield TraceContext(span, flowId, tracer, tags)

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
      headersList    <- headersAsList(headers)
      adapter        <- createTMAdapterFrom(headersList)
      spanContextOpt <- trySpanCtxExtraction(adapter, tracer)
      flowIdOpt      <- IO.pure(headers.get(CaseInsensitiveString("flow_id")).map(_.value))
      spanBuilder    <- buildSpan(rootSpanName, ("flow_id" -> FlowId.from(flowIdOpt).value) :: headersList, tracer)
      rootSpan       <- IO.delay(spanContextOpt.map(ctx => spanBuilder.asChildOf(ctx)).getOrElse(spanBuilder).start())
    } yield TraceContext(rootSpan, FlowId.from(flowIdOpt), tracer, headersList)

  private def headersAsList(headers: org.http4s.Headers): IO[List[(String, String)]] =
    IO.delay(
      headers.toList.map { header =>
        header.name.toString() -> header.value.toString()
      }
    )

  private def createTMAdapterFrom(headersList: List[(String, String)]): IO[TextMapExtractAdapter] =
    IO.delay(
      new TextMapExtractAdapter(headersList.toMap.asJava)
    )

  private def trySpanCtxExtraction(adapter: TextMapExtractAdapter, tracer: Tracer): IO[Option[SpanContext]] =
    Try(tracer.extract(Format.Builtin.HTTP_HEADERS, adapter)) match {
      case Success(spanContext) => IO.pure(Some(spanContext))
      case Failure(exception) =>
        logger.warn(s"Error extracting span context: ${exception.getMessage}")
        IO.pure(None)
    }
}

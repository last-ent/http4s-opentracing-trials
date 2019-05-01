package com.example.http4sopentracing.tracing

import io.opentracing.{Span, Tracer}

trait LocalTracer {
  def createRootSpan(rootSpanName: String): Span
  def createChildSpan(spanName:    String, rootSpan: Span): Span

  def logError(span:  Span, exception: Throwable): Span
  def closeSpan(span: Span)
}

class LocalTracerImpl(val tracer: Tracer) extends LocalTracer {
  def createRootSpan(rootSpanName: String): Span = ???
  def createChildSpan(spanName:    String, rootSpan: Span): Span = ???

  def logError(span:  Span, exception: Throwable): Span = ???
  def closeSpan(span: Span) = ???
}

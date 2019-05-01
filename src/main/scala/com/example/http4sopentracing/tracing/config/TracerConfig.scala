package com.example.http4sopentracing.tracing.config

import io.opentracing.{Span}
import java.util.UUID

import com.example.http4sopentracing.tracing.LocalTracer

case class TraceContext(flowId: FlowId, span: Span, localTracer: LocalTracer)

case class FlowId(value: String) extends AnyVal

object FlowId {
  def from(idOpt: Option[String]): FlowId = idOpt.map(FlowId.apply).getOrElse(getUUID())

  def getUUID(): FlowId = FlowId(UUID.randomUUID().toString)
}

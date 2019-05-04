package com.example.http4sopentracing.tracer

import java.util.UUID

case class FlowId(value: String) extends AnyVal

object FlowId {
  def from(idOpt: Option[String]): FlowId = FlowId(idOpt.getOrElse(getUUID()))

  private def getUUID(): String = UUID.randomUUID().toString
}

package com.example.http4sopentracing.tracing.config

case class JaegerConfig(
    serviceName:    String,
    samplerConfig:  SamplerConfig,
    reporterConfig: ReporterConfig
)

case class SamplerConfig(samplerType: SamplerType, samplerParam: Int)

sealed trait SamplerType {
  val value: String
}

object SamplerType {
  case object ConstSamplerType extends SamplerType {
    val value = "const"
  }

  def apply(samplerType: String): SamplerType =
    samplerType match {
      case _ => ConstSamplerType
    }
}

case class ReporterConfig(logSpans: Boolean) extends AnyVal

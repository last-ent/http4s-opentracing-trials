package com.example.http4sopentracing.tracing

import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration}

import com.example.http4sopentracing.tracing.config.JaegerConfig

object TracerBuilder {
  def fromJaeger(jaegerConfig: JaegerConfig): LocalTracer = {
    val samplerConfig = SamplerConfiguration
      .fromEnv()
      .withType(jaegerConfig.samplerConfig.samplerType.value)
      .withParam(jaegerConfig.samplerConfig.samplerParam)

    val reporterConfig = ReporterConfiguration
      .fromEnv()
      .withLogSpans(jaegerConfig.reporterConfig.logSpans)

    val config = Configuration
      .fromEnv()
      .withServiceName(jaegerConfig.serviceName)
      .withSampler(samplerConfig)
      .withReporter(reporterConfig)

    new LocalTracerImpl(config.getTracer)
  }
}

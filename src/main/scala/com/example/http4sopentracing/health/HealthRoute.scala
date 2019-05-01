package com.example.http4sopentracing.health

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._

object HealthRoute {
  val healthCheck = HttpRoutes
    .of[IO] {
      case GET -> Root / "health" =>
        Ok("Ok.")
    }
}

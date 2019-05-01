package com.example.http4sopentracing.health

import org.scalatest.{Matchers, WordSpec}
import org.http4s._
import cats.effect.IO
import com.example.http4sopentracing.Server

class HealthRouteSpec extends WordSpec with Matchers {
  "HealthRoute" should {
    """return 200 OK. (GET /health)""" in {
      val getHealth = Request[IO](Method.GET, Uri.uri("/health"))
      val result    = Server.httpApp.run(getHealth).unsafeRunSync()

      result.status shouldEqual Status.Ok
      result.as[String].unsafeRunSync() == "Ok."
    }
  }
}

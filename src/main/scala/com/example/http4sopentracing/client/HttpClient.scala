package com.example.http4sopentracing.client

import cats.effect.IO
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend

class HttpClient {
  implicit val backend = AsyncHttpClientCatsBackend[cats.effect.IO]()

  def get(url: Uri, headers: Map[String, String] = Map.empty): IO[StatusCode] =
    sttp
      .get(url)
      .headers(headers)
      .send()
      .map(_.code)
}

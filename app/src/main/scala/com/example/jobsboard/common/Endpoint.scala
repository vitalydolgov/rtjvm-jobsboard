package com.example.jobsboard.common

import tyrian.*
import tyrian.http.*
import cats.effect.IO
import io.circe.Encoder
import io.circe.syntax.*

import com.example.jobsboard.core.*

trait Endpoint[M] {
  val location: String
  val method: Method
  val onResponse: Response => M
  val onError: HttpError => M

  private def call[A: Encoder](payload: A, authorization: Option[String]): Cmd[IO, M] =
    Http.send(
      Request(
        url = location,
        method = method,
        headers = authorization.map(token => Header("authorization", token)).toList,
        body = Body.json(payload.asJson.toString),
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )

  private def call(authorization: Option[String]): Cmd[IO, M] =
    Http.send(
      Request(
        url = location,
        method = method,
        headers = authorization.map(token => Header("authorization", token)).toList,
        body = Body.Empty,
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )

  def call[A: Encoder](payload: A): Cmd[IO, M] =
    call(payload, None)

  def call(): Cmd[IO, M] =
    call(authorization = None)

  def callAuthorized[A: Encoder](payload: A): Cmd[IO, M] =
    call(payload, Session.getToken())

  def callAuthorized(): Cmd[IO, M] =
    call(authorization = Session.getToken())
}

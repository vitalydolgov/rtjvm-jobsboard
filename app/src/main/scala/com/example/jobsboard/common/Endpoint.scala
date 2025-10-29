package com.example.jobsboard.common

import tyrian.*
import tyrian.http.*
import cats.effect.IO
import io.circe.Encoder
import io.circe.syntax.*
import io.circe.parser.*

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

object Endpoint {
  def onResponse[A: io.circe.Decoder, M](
      onSuccess: A => M,
      onError: String => M
  ): Response => M = resp => {
    resp.status match {
      case Status(code, _) if code >= 200 && code < 300 =>
        val rawJson = resp.body
        val parsedJson = parse(rawJson).flatMap(_.as[A])

        parsedJson match {
          case Left(err)    => onError(s"Parsing error: ${err.toString}")
          case Right(value) => onSuccess(value)
        }
      case Status(_, message) => onError(s"Error: $message")
    }
  }

  def onResponseText[M](
      onSuccess: String => M,
      onError: String => M
  ): Response => M = resp =>
    resp.status match {
      case Status(code, _) if code >= 200 && code < 300 =>
        onSuccess(resp.body)
      case Status(code, _) if code >= 400 && code < 500 =>
        val rawJson = resp.body
        val parsedJson = parse(rawJson).flatMap(_.hcursor.get[String]("error"))
        parsedJson match {
          case Left(error)    => onError(s"Error: ${error.getMessage}")
          case Right(message) => onError(message)
        }
      case _ => onError("Unknown error.")
    }
}

package com.example.jobsboard.http

import cats.effect.Concurrent
import cats.implicits.*

import com.example.jobsboard.http.routes.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F].routes

  val endpoints = Router {
    "/api" -> (healthRoutes <+> jobRoutes)
  }
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger] = new HttpApi[F]
}

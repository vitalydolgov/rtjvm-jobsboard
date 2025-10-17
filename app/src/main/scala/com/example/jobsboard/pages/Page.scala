package com.example.jobsboard.pages

import tyrian.*
import cats.effect.IO

object Page {
  trait Message

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }

  final case class Status(message: String, kind: StatusKind)

  object Urls {
    val EMPTY = ""
    val HOME = "/"
    val LOGIN = "/login"
    val SIGNUP = "/signup"
    val FORGOT_PASSWORD = "/forgot-password"
    val RECOVER_PASSWORD = "/recover-password"
    val JOBS = "/jobs"
  }

  import Urls.*

  def get(location: String): Page = location match {
    case `LOGIN`                   => LoginPage()
    case `SIGNUP`                  => SignUpPage()
    case `FORGOT_PASSWORD`         => ForgotPasswordPage()
    case `RECOVER_PASSWORD`        => RecoverPasswordPage()
    case `EMPTY` | `HOME` | `JOBS` => JobListPage()
    case s"/jobs/$id"              => JobPage(id)
    case _                         => NotFoundPage()
  }

}

abstract class Page {
  import Page.*

  def initCommand: Cmd[IO, Message]
  def update(message: Message): (Page, Cmd[IO, Message])
  def view: Html[Message]
}

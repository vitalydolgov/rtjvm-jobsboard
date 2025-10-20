package com.example.jobsboard.pages

import tyrian.*
import cats.effect.IO

import com.example.jobsboard.*

object Page {
  trait Message extends App.Message

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
    val HASH = "#"
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
  def initCommand: Cmd[IO, App.Message]
  def update(message: App.Message): (Page, Cmd[IO, App.Message])
  def view: Html[App.Message]
}

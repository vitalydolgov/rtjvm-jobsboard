package com.example.jobsboard.pages

import tyrian.*
import cats.effect.IO

import com.example.jobsboard.*
import com.example.jobsboard.components.*

object Page {
  trait Message extends App.Message

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }

  final case class Status(message: String, kind: StatusKind)

  object Status {
    val LOADING = Page.Status("Loading", Page.StatusKind.LOADING)
  }

  object Urls {
    val EMPTY = ""
    val HOME = "/"
    val LOGIN = "/login"
    val SIGNUP = "/signup"
    val FORGOT_PASSWORD = "/forgot-password"
    val RESET_PASSWORD = "/reset-password"
    val POST_JOB = "/post-job"
    val JOBS = "/jobs"
    val PROFILE = "/profile"
    val HASH = "#"
    def JOB(id: String) = s"/jobs/$id"
  }

  import Urls.*

  def get(location: String): Page = location match {
    case `LOGIN`                   => LoginPage()
    case `SIGNUP`                  => SignUpPage()
    case `FORGOT_PASSWORD`         => ForgotPasswordPage()
    case `RESET_PASSWORD`          => ResetPasswordPage()
    case `POST_JOB`                => PostJobPage()
    case `EMPTY` | `HOME` | `JOBS` => JobListPage()
    case `PROFILE`                 => ProfilePage()
    case s"/jobs/$id"              => JobPage(id)
    case _                         => NotFoundPage()
  }

}

abstract class Page extends Component[App.Message, Page]

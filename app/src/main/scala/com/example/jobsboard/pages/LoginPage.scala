package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import io.circe.generic.auto.*
import tyrian.cmds.Logger

import cats.effect.IO

import com.example.jobsboard.*
import com.example.jobsboard.core.*
import com.example.jobsboard.common.*
import com.example.jobsboard.components.*
import com.example.jobsboard.domain.auth.*

object LoginPage {
  trait Message extends App.Message
  case class UpdateEmail(email: String) extends Message
  case class UpdatePassword(password: String) extends Message
  case class LoginSuccess(token: String) extends Message
  case class LoginError(message: String) extends Message
  case object AttemptLogin extends Message
  case object NoOp extends Message

  object Endpoints {
    val login = new Endpoint[Message] {
      override val location: String = Constants.endpoints.login
      override val method: Method = Method.Post

      override val onResponse: Response => Message =
        resp => {
          val tokenOpt = resp.headers.get("authorization")
          tokenOpt match {
            case Some(token) => LoginSuccess(token)
            case None        => LoginError("Invalid username or password.")
          }
        }

      override val onError: HttpError => Message =
        err => LoginError(err.toString)
    }
  }

  object Commands {
    def login(payload: LoginPayload): Cmd[IO, Message] = {
      Endpoints.login.call(payload)
    }
  }
}

final case class LoginPage(
    email: String = "",
    password: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Log In", status) {

  import LoginPage.*

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = message match {
    case UpdateEmail(email)       => (this.copy(email = email), Cmd.None)
    case UpdatePassword(password) => (this.copy(password = password), Cmd.None)
    case AttemptLogin =>
      if (!email.matches(Constants.emailRegex)) (setErrorStatus("Invalid email."), Cmd.None)
      else if (password.isEmpty) (setErrorStatus("Please enter a password."), Cmd.None)
      else (this, Commands.login(LoginPayload(email, password)))
    case LoginError(error) => (setErrorStatus(error), Cmd.None)
    case LoginSuccess(token) =>
      (setSuccessStatus("Success!"), Cmd.Emit(Session.SetToken(email, token, isNewSession = true)))
    case _ => (this, Cmd.None)
  }

  override def content: List[Html[App.Message]] = List(
    formInput("Email", "email", "text", true, UpdateEmail(_)),
    formInput("Password", "password", "password", true, UpdatePassword(_)),
    button(`type` := "button", onClick(AttemptLogin))("Log In"),
    Anchors.simpleNavLink("Forgot password?", Page.Urls.FORGOT_PASSWORD)
  )
}

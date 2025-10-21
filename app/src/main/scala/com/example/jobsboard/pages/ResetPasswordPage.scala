package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO
import io.circe.parser.*
import io.circe.generic.auto.*

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.domain.auth.*

object ResetPasswordPage {
  trait Message extends App.Message
  case class UpdateEmail(email: String) extends Message
  case class UpdateToken(token: String) extends Message
  case class UpdateNewPassword(password: String) extends Message
  case object AttemptResetPassword extends Message
  case object ResetPasswordSuccess extends Message
  case class ResetPasswordError(message: String) extends Message

  object Endpoints {
    val resetPassword = new Endpoint[Message] {
      override val location: String = Constants.endpoints.resetPassword
      override val method: Method = Method.Post
      override val onResponse: Response => Message = resp =>
        resp.status match {
          case Status(200, _) => ResetPasswordSuccess
          case Status(status, _) if status >= 400 && status < 500 =>
            val rawJson = resp.body
            val parsedJson = parse(rawJson).flatMap(_.hcursor.get[String]("error"))
            parsedJson match {
              case Left(error)    => ResetPasswordError(s"Error: ${error.getMessage}")
              case Right(message) => ResetPasswordError(message)
            }
          case Status(_, message) => ResetPasswordError(message)
        }
      override val onError: HttpError => Message =
        err => ResetPasswordError(err.toString)
    }
  }

  object Commands {
    def resetPassword(email: String, token: String, newPassword: String): Cmd[IO, Message] =
      Endpoints.resetPassword.call(RecoverPasswordPayload(email, token, newPassword))
  }
}

final case class ResetPasswordPage(
    email: String = "",
    token: String = "",
    newPassword: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Reset Password", status) {

  import ResetPasswordPage.*

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = message match {
    case UpdateEmail(email)          => (this.copy(email = email), Cmd.None)
    case UpdateToken(token)          => (this.copy(token = token), Cmd.None)
    case UpdateNewPassword(password) => (this.copy(newPassword = password), Cmd.None)
    case AttemptResetPassword =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Please enter a valid email."), Cmd.None)
      else if (token.isEmpty)
        (setErrorStatus("Please enter a recovery token."), Cmd.None)
      else if (newPassword.isEmpty)
        (setErrorStatus("Please enter a new password."), Cmd.None)
      else
        (this, Commands.resetPassword(email, token, newPassword))
    case ResetPasswordSuccess        => (setSuccessStatus("You may log in now."), Cmd.None)
    case ResetPasswordError(message) => (setErrorStatus(message), Cmd.None)
    case _                           => (this, Cmd.None)
  }

  override def content: List[Html[App.Message]] = List(
    formInput("Email", "email", "text", true, UpdateEmail(_)),
    formInput("Recover Token", "token", "text", true, UpdateToken(_)),
    formInput("New Password", "password", "password", true, UpdateNewPassword(_)),
    button(`type` := "button", onClick(AttemptResetPassword))("Change Password"),
    auxLink(Page.Urls.FORGOT_PASSWORD, "Don't have a token yet?")
  )
}

package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO
import io.circe.generic.auto.*

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.components.*
import com.example.jobsboard.domain.auth.*

object ForgotPasswordPage {
  trait Message extends App.Message
  case class UpdateEmail(email: String) extends Message
  case object AttemptResetPassword extends Message
  case object PasswordResetSuccess extends Message
  case class PasswordResetError(message: String) extends Message

  object Endpoints {
    val forgotPassword = new Endpoint[Message] {
      override val location: String = Constants.endpoints.forgotPassword
      override val method: Method = Method.Post

      override val onResponse: Response => Message = _.status match {
        case Status(200, _) => PasswordResetSuccess
        case _              => PasswordResetError("Unknown error.")
      }

      override val onError: HttpError => Message =
        err => PasswordResetError(err.toString)
    }
  }

  object Commands {
    def forgotPassword(email: String): Cmd[IO, Message] =
      Endpoints.forgotPassword.call(ForgotPasswordPayload(email))
  }
}

final case class ForgotPasswordPage(
    email: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Reset Password", status) {

  import ForgotPasswordPage.*

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = message match {
    case UpdateEmail(email) => (this.copy(email = email), Cmd.None)
    case AttemptResetPassword =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Invalid email."), Cmd.None)
      else
        (this, Commands.forgotPassword(email))
    case PasswordResetSuccess        => (setSuccessStatus("Check your mailbox."), Cmd.None)
    case PasswordResetError(message) => (setErrorStatus(message), Cmd.None)
    case _                           => (this, Cmd.None)
  }

  override def content: List[Html[App.Message]] = List(
    formInput("Email", "email", "text", true, UpdateEmail(_)),
    button(`type` := "button", onClick(AttemptResetPassword))("Send Email"),
    Anchors.simpleNavLink("Have a token?", Page.Urls.RESET_PASSWORD, "auth-link")
  )

}

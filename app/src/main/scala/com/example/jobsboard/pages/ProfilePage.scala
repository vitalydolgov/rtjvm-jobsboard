package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO
import io.circe.generic.auto.*

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.core.*
import com.example.jobsboard.domain.auth.*

object ProfilePage {
  trait Message extends App.Message
  case class UpdateOldPassword(password: String) extends Message
  case class UpdateNewPassword(password: String) extends Message
  case object AttemptChangePassword extends Message
  case object ChangePasswordSuccess extends Message
  case class ChangePasswordError(message: String) extends Message

  object Endpoints {
    val changePassword = new Endpoint[Message] {
      override val location: String = Constants.endpoints.changePassword
      override val method: Method = Method.Put

      override val onResponse: Response => Message = resp =>
        resp.status match {
          case Status(200, _) => ChangePasswordSuccess
          case Status(status, _) if status >= 400 && status < 500 =>
            ChangePasswordError("Password is incorrect.")
          case _ => ChangePasswordError("Unknown error.")
        }

      override val onError: HttpError => Message =
        err => ChangePasswordError(err.toString)
    }
  }

  object Commands {
    def changePassword(oldPassword: String, newPassword: String): Cmd[IO, Message] =
      Endpoints.changePassword.callAuthorized(NewPasswordPayload(oldPassword, newPassword))
  }
}

final case class ProfilePage(
    oldPassword: String = "",
    newPassword: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Profile", status) {

  import ProfilePage.*

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = message match {
    case UpdateOldPassword(password) => (this.copy(oldPassword = password), Cmd.None)
    case UpdateNewPassword(password) => (this.copy(newPassword = password), Cmd.None)
    case AttemptChangePassword =>
      if (oldPassword.isEmpty || newPassword.isEmpty)
        (setErrorStatus("Please enter passwords."), Cmd.None)
      else
        (this, Commands.changePassword(oldPassword, newPassword))
    case ChangePasswordError(message) => (setErrorStatus(message), Cmd.None)
    case ChangePasswordSuccess        => (setSuccessStatus("Password has been changed."), Cmd.None)
    case _                            => (this, Cmd.None)
  }

  override def content: List[Html[App.Message]] = List(
    formInput("Current Password", "old-password", "password", true, UpdateOldPassword(_)),
    formInput("New Password", "new-password", "password", true, UpdateNewPassword(_)),
    button(`type` := "button", onClick(AttemptChangePassword))("Change Password")
  )

  private def notLoggedInView: Html[App.Message] =
    div(`class` := "not-logged-in")("You're not logged in yet.")

  override def view: Html[App.Message] =
    if (Session.isActive) super.view
    else notLoggedInView
}

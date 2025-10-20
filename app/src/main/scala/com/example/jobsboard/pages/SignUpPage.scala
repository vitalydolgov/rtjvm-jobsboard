package com.example.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.domain.auth.*

object SignUpPage {
  trait Message extends App.Message
  case class UpdateEmail(email: String) extends Message
  case class UpdatePassword(password: String) extends Message
  case class UpdateConfirmPassword(confirmPassword: String) extends Message
  case class UpdateFirstName(firstName: String) extends Message
  case class UpdateLastName(lastName: String) extends Message
  case class UpdateCompany(company: String) extends Message
  case class SignUpSuccess(message: String) extends Message
  case class SignUpError(message: String) extends Message
  case object AttemptSignUp extends Message
  case object NoOp extends Message

  object Endpoints {
    val signup = new Endpoint[Message] {
      override val location: String = Constants.endpoints.signup
      override val method: Method = Method.Post

      override val onResponse: Response => Message =
        resp =>
          resp.status match {
            case Status(201, _) => SignUpSuccess("Success! Log in now.")
            case Status(status, _) if status >= 400 && status < 500 =>
              val json = resp.body
              val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
              parsed match {
                case Left(err)  => SignUpError(s"Error: ${err.getMessage}")
                case Right(err) => SignUpError(err)
              }
            case _ => ???
          }

      override val onError: HttpError => Message =
        err => SignUpError(err.toString)
    }
  }

  object Commands {
    def signup(payload: NewUserPayload): Cmd[IO, Message] = {
      Endpoints.signup.call(payload)
    }
  }
}

final case class SignUpPage(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    firstName: String = "",
    lastName: String = "",
    company: String = "",
    status: Option[Page.Status] = None
) extends Page {
  import SignUpPage.*

  override def initCommand: Cmd[IO, App.Message] = Cmd.None

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = message match {
    case UpdateEmail(email)              => (this.copy(email = email), Cmd.None)
    case UpdatePassword(password)        => (this.copy(password = password), Cmd.None)
    case UpdateConfirmPassword(password) => (this.copy(confirmPassword = password), Cmd.None)
    case UpdateFirstName(firstName)      => (this.copy(firstName = firstName), Cmd.None)
    case UpdateLastName(lastName)        => (this.copy(lastName = lastName), Cmd.None)
    case UpdateCompany(company)          => (this.copy(company = company), Cmd.None)
    case AttemptSignUp =>
      if (!email.matches(Constants.emailRegex)) (setErrorStatus("Email is invalid."), Cmd.None)
      else if (password.isEmpty) (setErrorStatus("Please provide a password."), Cmd.None)
      else if (password != confirmPassword)
        (setErrorStatus("Passwords don't match."), Cmd.None)
      else
        (
          this,
          Commands.signup(
            NewUserPayload(
              email,
              password,
              Option(firstName).filter(_.nonEmpty),
              Option(lastName).filter(_.nonEmpty),
              Option(company).filter(_.nonEmpty)
            )
          )
        )
    case SignUpError(message)   => (setErrorStatus(message), Cmd.None)
    case SignUpSuccess(message) => (setSuccessStatus(message), Cmd.None)
    case _                      => (this, Cmd.None)
  }

  private def formInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => Message
  ) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  override def view: Html[App.Message] =
    div(`class` := "form-section")(
      div(`class` := "top-section")(
        h1("Sign Up")
      ),
      form(
        name := "signup",
        `class` := "form",
        onEvent(
          "submit",
          e => {
            e.preventDefault()
            NoOp
          }
        )
      )(
        formInput("Email", "email", "text", true, UpdateEmail(_)),
        formInput("Password", "password", "password", true, UpdatePassword(_)),
        formInput("Confirm Password", "confirm", "password", true, UpdateConfirmPassword(_)),
        formInput("First Name", "email", "text", false, UpdateFirstName(_)),
        formInput("Last Name", "email", "text", false, UpdateLastName(_)),
        formInput("Company", "email", "text", false, UpdateCompany(_)),
        button(`type` := "button", onClick(AttemptSignUp))("Sign Up")
      ),
      status.map(s => div(s.message)).getOrElse(div())
    )
}

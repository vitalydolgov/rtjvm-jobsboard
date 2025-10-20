package com.example.jobsboard.core

import tyrian.*
import tyrian.http.*
import cats.effect.IO
import tyrian.cmds.Logger
import org.scalajs.dom.document
import scala.scalajs.js

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.pages.*

object Session {
  trait Message extends App.Message
  case class SetToken(email: String, token: String, isNewSession: Boolean) extends Message
  case object Logout extends Message
  case object LogoutSuccess extends Message
  case object LogoutError extends Message

  private def getCookie(name: String): Option[String] = {
    document.cookie
      .split(";")
      .map(_.trim)
      .find(_.startsWith(s"$name="))
      .map(_.split("="))
      .map(_(1))
  }

  def getToken(): Option[String] = getCookie(Constants.cookies.tokenKey)

  def isActive: Boolean = getToken().nonEmpty

  object Endpoints {
    val logout = new Endpoint[Message] {
      val location: String = Constants.endpoints.logout
      val method: Method = Method.Post
      val onResponse: Response => Message = _ => LogoutSuccess
      val onError: HttpError => Message = _ => LogoutError
    }
  }

  object Commands {
    def setSessionCookie(
        name: String,
        value: String,
        isNew: Boolean = false
    ): Cmd[IO, Message] =
      Cmd.SideEffect[IO] {
        if (getCookie(name).isEmpty || isNew)
          document.cookie =
            s"$name=$value;expires=${new js.Date(js.Date.now() + Constants.cookies.duration)};path=/"
      }

    def setAllSessionCookies(
        email: String,
        token: String,
        isNew: Boolean = false
    ): Cmd[IO, Message] = {
      setSessionCookie(Constants.cookies.emailKey, email, isNew) |+|
        setSessionCookie(Constants.cookies.tokenKey, token, isNew)
    }

    def clearSessionCookie(name: String): Cmd[IO, Message] =
      Cmd.SideEffect[IO] {
        document.cookie = s"$name=;expires=${new js.Date(0)};path=/"
      }

    def clearAllSessionCookies(): Cmd[IO, Message] = {
      clearSessionCookie(Constants.cookies.emailKey) |+|
        clearSessionCookie(Constants.cookies.tokenKey)
    }

    def logout(): Cmd[IO, Message] =
      Endpoints.logout.callAuthorized()
  }
}

final case class Session(
    email: Option[String] = None,
    token: Option[String] = None
) {
  import Session.*

  def update(message: Message): (Session, Cmd[IO, App.Message]) = message match {
    case SetToken(email, token, isNewSession) =>
      val cookieCommand = Commands.setAllSessionCookies(email, token, isNewSession)
      val routingCommand =
        if (isNewSession) Cmd.Emit(Router.ChangeLocation(Page.Urls.HOME))
        else Cmd.None

      (
        this.copy(email = Some(email), token = Some(token)),
        cookieCommand |+| routingCommand
      )
    case Logout =>
      (this, token.map(_ => Commands.logout()).getOrElse(Cmd.None))
    case LogoutSuccess =>
      val cookieCommand = Commands.clearAllSessionCookies()
      val routerCommand = Cmd.Emit(Router.ChangeLocation(Page.Urls.HOME))
      (this.copy(email = None, token = None), cookieCommand |+| routerCommand)
  }

  def initCommand: Cmd[IO, Message] = {
    val commandOpt = for {
      email <- getCookie(Constants.cookies.emailKey)
      token <- getCookie(Constants.cookies.tokenKey)
    } yield Cmd.Emit(SetToken(email, token, isNewSession = false))

    commandOpt.getOrElse(Cmd.None)
  }
}

package com.example.jobsboard.core

import tyrian.*
import cats.effect.*
import fs2.dom.History
import org.scalajs.dom.window

import com.example.jobsboard.*

case class Router private (location: String, history: History[IO, String]) {
  import Router.*

  private def goTo[M](location: String): Cmd[IO, M] = {
    Cmd.SideEffect[IO] {
      history.pushState(location, location)
    }
  }

  private def cleanUrlIfNeeded(url: String): String =
    if (url.startsWith("\""))
      url.substring(1, url.length - 1)
    else url

  def update(message: Message): (Router, Cmd[IO, Message]) = message match {
    case ChangeLocation(newLocation, browserTriggered) =>
      if (location == newLocation) (this, Cmd.None)
      else {
        val historyCommand =
          if (browserTriggered) Cmd.None
          else goTo(newLocation)
        (this.copy(location = newLocation), historyCommand)
      }
    case ExternalRedirect(location) =>
      window.location.href = cleanUrlIfNeeded(location)
      (this, Cmd.None)
    case _ => (this, Cmd.None)
  }
}

object Router {
  trait Message extends App.Message
  case class ChangeLocation(location: String, browserTriggered: Boolean = false) extends Message
  case class ExternalRedirect(location: String) extends Message

  def startAt[M](initialLocation: String): (Router, Cmd[IO, M]) = {
    val router = Router(initialLocation, History[IO, String])
    (router, router.goTo(initialLocation))
  }
}

package com.example.jobsboard

import tyrian.*
import tyrian.Html.*
import cats.effect.*
import scala.scalajs.js.annotation.*
import org.scalajs.dom.window
import scala.concurrent.duration.*

import com.example.jobsboard.core.*
import com.example.jobsboard.core.Router.ChangeLocation

object App {
  type Message = Router.Message

  case class Model(router: Router)
}

@JSExportTopLevel("JobsboardApp")
class App extends TyrianApp[App.Message, App.Model] {
  import App.*
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Message]) = {
    val (router, command) = Router.startAt(window.location.pathname)
    (Model(router), command)
  }

  override def subscriptions(model: Model): Sub[IO, Message] =
    Sub.make(
      "urlChange",
      model.router.history.state.discrete
        .map(_.get)
        .map(newLocation => Router.ChangeLocation(newLocation, true))
    )

  override def update(model: Model): Message => (Model, Cmd[IO, Message]) = {
    case message: Router.Message =>
      val (newRouter, command) = model.router.update(message)
      (Model(newRouter), command)
  }

  private def navLink(text: String, location: String) =
    a(
      href := location,
      `class` := "nav-link",
      onEvent(
        "click",
        e => {
          e.preventDefault()
          Router.ChangeLocation(location)
        }
      )
    )(text)

  override def view(model: Model): Html[Message] =
    div(
      navLink("Jobs", "/jobs"),
      navLink("Login", "/login"),
      navLink("Sign Up", "/signup"),
      div(s"You are now at: ${model.router.location}")
    )
}

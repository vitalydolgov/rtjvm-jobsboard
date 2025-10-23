package com.example.jobsboard

import tyrian.*
import tyrian.Html.*
import cats.effect.*
import scala.scalajs.js.annotation.*
import org.scalajs.dom.window
import scala.concurrent.duration.*

import com.example.jobsboard.core.*
import com.example.jobsboard.components.*
import com.example.jobsboard.pages.*

object App {
  trait Message
  case object NoOp extends Message

  case class Model(router: Router, session: Session, page: Page)
}

@JSExportTopLevel("JobsboardApp")
class App extends TyrianApp[App.Message, App.Model] {
  import App.*

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Message]) = {
    val location = window.location.pathname
    val page = Page.get(location)
    val pageCommand = page.initCommand
    val (router, routerCommand) = Router.startAt(location)
    val session = Session()
    val sessionCommand = session.initCommand
    (Model(router, session, page), routerCommand |+| sessionCommand |+| pageCommand)
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
      val (newRouter, newRouterCommand) = model.router.update(message)
      if (model.router == newRouter) (model, Cmd.None)
      else {
        val newPage = Page.get(newRouter.location)
        val newPageCommand = newPage.initCommand
        (model.copy(router = newRouter, page = newPage), newRouterCommand |+| newPageCommand)
      }
    case message: Session.Message =>
      val (newSession, command) = model.session.update(message)
      (model.copy(session = newSession), command)
    case message: App.Message =>
      val (newPage, command) = model.page.update(message)
      (model.copy(page = newPage), command)
  }

  override def view(model: Model): Html[Message] =
    div(
      Header.view,
      model.page.view
    )
}

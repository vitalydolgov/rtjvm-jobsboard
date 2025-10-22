package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO
import org.scalajs.dom.*
import scala.concurrent.duration.FiniteDuration

import com.example.jobsboard.*
import com.example.jobsboard.core.*

abstract class FormPage(title: String, status: Option[Page.Status]) extends Page {

  private def clearForm(): Cmd[IO, App.Message] =
    Cmd.Run[IO, Unit, App.Message] {
      def effect(retriesLeft: Int): IO[Option[HTMLFormElement]] = for {
        formOpt <- IO(Option(document.getElementById("form").asInstanceOf[HTMLFormElement]))
        renderedForm <-
          if (formOpt.isEmpty && retriesLeft > 0)
            IO.sleep(FiniteDuration(100, "millis")) *> effect(retriesLeft - 1)
          else IO(formOpt)
      } yield renderedForm

      effect(10).map(_.foreach(_.reset()))
    }(_ => App.NoOp)

  override def initCommand: Cmd[IO, App.Message] = clearForm()

  protected def content: List[Html[App.Message]]

  override def view: Html[App.Message] =
    div(`class` := "form-section")(
      div(`class` := "top-section")(
        h1(title)
      ),
      form(
        `class` := "form",
        id := "form",
        onEvent(
          "submit",
          e => {
            e.preventDefault()
            App.NoOp
          }
        )
      )(
        content
      ),
      status.map(s => div(s.message)).getOrElse(div())
    )

  protected def formInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => App.Message
  ) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  protected def auxLink(location: String, text: String): Html[App.Message] =
    a(
      href := location,
      `class` := "aux-link",
      onEvent(
        "click",
        e => {
          e.preventDefault()
          Router.ChangeLocation(location)
        }
      )
    )(text)
}

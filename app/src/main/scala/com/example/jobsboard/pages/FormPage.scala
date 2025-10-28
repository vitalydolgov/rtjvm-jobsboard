package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO
import org.scalajs.dom.*
import scala.concurrent.duration.FiniteDuration

import com.example.jobsboard.*
import com.example.jobsboard.core.*
import com.example.jobsboard.common.*

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

  private def statusOpt =
    status.map(s => div(s.message)).getOrElse(div())

  override def view: Html[App.Message] =
    div(`class` := "row")(
      div(`class` := "col-md-5 p-0")(
        div(`class` := "logo")(
          img(src := Constants.logoImage)
        )
      ),
      div(`class` := "col-md-7")(
        div(`class` := "form-section")(
          div(`class` := "top-section")(
            h1(span(title)),
            statusOpt
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
          )
        )
      )
    )

  protected def formInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => App.Message
  ) =
    div(`class` := "row")(
      div(`class` := "col-md-12")(
        div(`class` := "form-input")(
          label(`for` := uid, `class` := "form-label")(
            if (isRequired) span("*") else span(),
            text(name)
          ),
          input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
        )
      )
    )

  protected def formTextArea(
      name: String,
      uid: String,
      isRequired: Boolean,
      onChange: String => App.Message
  ) =
    div(`class` := "form-input")(
      label(`for` := uid, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      textarea(`class` := "form-control", id := uid, onInput(onChange))("")
    )

  protected def formImageUpload(
      name: String,
      uid: String,
      image: Option[String],
      onChange: Option[File] => App.Message
  ) =
    div(`class` := "form-input")(
      label(`for` := uid, `class` := "form-label")(name),
      input(
        `type` := "file",
        `class` := "form-control",
        id := uid,
        accept := "image/*",
        onEvent(
          "change",
          e => {
            val imageInput = e.target.asInstanceOf[HTMLInputElement]
            val fileList = imageInput.files
            if (fileList.length > 0)
              onChange(Some(fileList(0)))
            else
              onChange(None)
          }
        )
      ),
      img(
        id := "preview",
        src := image.getOrElse(""),
        alt := "Preview",
        width := "100",
        height := "100"
      )
    )
}

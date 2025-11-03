package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO

import com.example.jobsboard.*
import com.example.jobsboard.common.*

final case class NotFoundPage() extends Page {
  override def initCommand: Cmd[IO, App.Message] = Cmd.None
  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = (this, Cmd.None)
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
            h1(span("Ouch!")),
            div("This page doesn't exist.")
          )
        )
      )
    )
}

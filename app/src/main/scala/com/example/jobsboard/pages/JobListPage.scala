package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO

import com.example.jobsboard.*

final case class JobListPage() extends Page {
  override def initCommand: Cmd[IO, App.Message] = Cmd.None
  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = (this, Cmd.None)
  override def view: Html[App.Message] = div("Job list page - TODO")
}

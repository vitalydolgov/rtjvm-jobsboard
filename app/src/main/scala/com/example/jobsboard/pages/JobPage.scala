package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO

final case class JobPage(id: String) extends Page {
  override def initCommand: Cmd[IO, Page.Message] = Cmd.None
  override def update(message: Page.Message): (Page, Cmd[IO, Page.Message]) = (this, Cmd.None)
  override def view: Html[Page.Message] = div(s"Job page $id - TODO")
}

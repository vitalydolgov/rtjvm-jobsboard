package com.example.jobsboard.components

import tyrian.*
import cats.effect.IO

import com.example.jobsboard.*

trait Component[Message, +Model] {
  def initCommand: Cmd[IO, Message]
  def update(message: Message): (Model, Cmd[IO, Message])
  def view: Html[Message]
}

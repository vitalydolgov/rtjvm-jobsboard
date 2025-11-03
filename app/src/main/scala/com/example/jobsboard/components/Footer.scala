package com.example.jobsboard.components

import tyrian.*
import tyrian.Html.*

import com.example.jobsboard.*

object Footer {
  def view: Html[App.Message] =
    div(`class` := "footer")(
      p(
        text("Written in "),
        a(href := "http://scala-lang.org", target := "blank")("Scala"),
        text(" with \u2764\uFE0F")
      )
    )
}

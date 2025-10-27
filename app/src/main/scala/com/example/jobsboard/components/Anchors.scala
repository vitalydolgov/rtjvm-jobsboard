package com.example.jobsboard.components

import tyrian.*
import tyrian.Html.*

import com.example.jobsboard.*
import com.example.jobsboard.core.*

object Anchors {
  def navLink(text: String, location: String)(locationToMessage: String => App.Message) = {
    li(`class` := "nav-item")(
      a(
        href := location,
        `class` := "nav-link",
        onEvent(
          "click",
          e => {
            e.preventDefault()
            locationToMessage(location)
          }
        )
      )(text)
    )
  }

  def simpleNavLink(text: String, location: String) = {
    navLink(text, location)(Router.ChangeLocation(_))
  }
}

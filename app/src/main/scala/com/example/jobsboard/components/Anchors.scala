package com.example.jobsboard.components

import tyrian.*
import tyrian.Html.*

import com.example.jobsboard.*
import com.example.jobsboard.core.*

object Anchors {
  def navLink(text: String, location: String, cssClass: String)(
      locationToMessage: String => App.Message
  ) = {
    a(
      href := location,
      `class` := cssClass,
      onEvent(
        "click",
        e => {
          e.preventDefault()
          locationToMessage(location)
        }
      )
    )(text)
  }

  def simpleNavLink(text: String, location: String, cssClass: String) = {
    navLink(text, location, cssClass)(Router.ChangeLocation(_))
  }
}

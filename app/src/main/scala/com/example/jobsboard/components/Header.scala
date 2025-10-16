package com.example.jobsboard.components

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.example.jobsboard.core.*

object Header {

  @js.native
  @JSImport("url:/static/images/logo.png", JSImport.Default)
  private val logoImage: String = js.native

  private def logo = {
    div(
      a(
        href := "/",
        onEvent(
          "click",
          e => {
            e.preventDefault()
            Router.ChangeLocation("/")
          }
        )
      )(
        img(
          `class` := "home-logo",
          src := logoImage,
          alt := "Jobs Board"
        )
      )
    )
  }

  private def navLink(text: String, location: String) = {
    li(`class` := "nav-item")(
      a(
        href := location,
        `class` := "nav-link",
        onEvent(
          "click",
          e => {
            e.preventDefault()
            Router.ChangeLocation(location)
          }
        )
      )(text)
    )
  }

  def view = {
    div(`class` := "header-container")(
      logo,
      div(`class` := "header-nav")(
        ul(`class` := "header-links")(
          navLink("Jobs", "/jobs"),
          navLink("Login", "/login"),
          navLink("Sign Up", "/signup")
        )
      )
    )
  }
}

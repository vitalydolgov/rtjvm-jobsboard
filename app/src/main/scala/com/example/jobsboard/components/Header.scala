package com.example.jobsboard.components

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.example.jobsboard.*
import com.example.jobsboard.core.*
import com.example.jobsboard.pages.*

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

  private def navLink(text: String, location: String)(locationToMessage: String => App.Message) = {
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

  private def simpleNavLink(text: String, location: String) = {
    navLink(text, location)(Router.ChangeLocation(_))
  }

  private def navLinks: List[Html[App.Message]] = {
    val constantLinks = List(
      simpleNavLink("Jobs", Page.Urls.JOBS)
    )

    val unauthedLinks = List(
      simpleNavLink("Login", Page.Urls.LOGIN),
      simpleNavLink("Sign Up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      navLink("Log Out", Page.Urls.HASH)(_ => Session.Logout)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
  }

  def view = {
    div(`class` := "header-container")(
      logo,
      div(`class` := "header-nav")(
        ul(`class` := "header-links")(
          navLinks
        )
      )
    )
  }
}

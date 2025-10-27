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

  private def navLinks: List[Html[App.Message]] = {
    val constantLinks = List(
      Anchors.simpleNavLink("Jobs", Page.Urls.JOBS),
      Anchors.simpleNavLink("Post a Job", Page.Urls.POST_JOB)
    )

    val unauthedLinks = List(
      Anchors.simpleNavLink("Login", Page.Urls.LOGIN),
      Anchors.simpleNavLink("Sign Up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      Anchors.simpleNavLink("Profile", Page.Urls.PROFILE),
      Anchors.navLink("Log Out", Page.Urls.HASH)(_ => Session.Logout)
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

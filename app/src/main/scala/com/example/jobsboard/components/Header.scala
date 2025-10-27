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
        `class` := "navbar-brand",
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

  private def navLink(text: String, location: String)(locationToMessage: String => App.Message) =
    li(`class` := "nav-item")(
      Anchors.navLink(text, location, "nav-link jvm-item")(locationToMessage)
    )

  private def simpleNavLink(text: String, location: String) =
    navLink(text, location)(Router.ChangeLocation(_))

  private def navLinks: List[Html[App.Message]] = {
    val constantLinks = List(
      simpleNavLink("Jobs", Page.Urls.JOBS),
      simpleNavLink("Post a Job", Page.Urls.POST_JOB)
    )

    val unauthedLinks = List(
      simpleNavLink("Login", Page.Urls.LOGIN),
      simpleNavLink("Sign Up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      simpleNavLink("Profile", Page.Urls.PROFILE),
      navLink("Log Out", Page.Urls.HASH)(_ => Session.Logout)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
  }

  def view = {
    div(`class` := "container-fluid p-0")(
      div(`class` := "jvm-nav")(
        div(`class` := "container")(
          nav(`class` := "navbar navbar-expand-lg navbar-light JVM-nav")(
            div(`class` := "container")(
              logo,
              button(
                `class` := "navbar-toggler",
                `type` := "button",
                attribute("data-bs-toggle", "collapse"),
                attribute("data-bs-target", "#navbarNav"),
                attribute("aria-controls", "navbarNav"),
                attribute("aria-expanded", "false"),
                attribute("aria-label", "Toggle navigation")
              )(
                span(`class` := "navbar-toggler-icon")()
              ),
              div(`class` := "collapse navbar-collapse", id := "navbarNav")(
                ul(
                  `class` := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3"
                )(
                  navLinks
                )
              )
            )
          )
        )
      )
    )
  }
}

package com.example.jobsboard.common

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Constants {
  @js.native
  @JSImport("url:/static/images/logo.png", JSImport.Default)
  val logoImage: String = js.native

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  val defaultPageSize = 20

  object endpoints {
    val root = "http://localhost:8081"
    val signup = s"$root/api/auth/users"
    val login = s"$root/api/auth/login"
    val logout = s"$root/api/auth/logout"
    val checkToken = s"$root/api/auth/checkToken"
    val forgotPassword = s"$root/api/auth/reset"
    val resetPassword = s"$root/api/auth/recover"
    val changePassword = s"$root/api/auth/users/password"
    val jobs = s"$root/api/jobs"
    val postJob = s"$root/api/jobs/create"
    val postJobPromoted = s"$root/api/jobs/promoted"
    val filters = s"$root/api/jobs/filters"
  }

  object cookies {
    val duration = 10 * 24 * 3600 * 1000 // 10 days
    val emailKey = "email"
    val tokenKey = "token"
  }
}

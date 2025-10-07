package com.example.jobsboard.domain

object auth {
  case class LoginInfo(
      email: String,
      password: String
  )

  case class NewPasswordInfo(
      oldPassword: String,
      newPassword: String
  )
}

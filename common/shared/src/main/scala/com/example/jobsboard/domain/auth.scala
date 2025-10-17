package com.example.jobsboard.domain

object auth {
  case class LoginPayload(
      email: String,
      password: String
  )

  case class NewUserPayload(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String]
  )

  case class NewPasswordPayload(
      oldPassword: String,
      newPassword: String
  )

  case class ForgotPasswordPayload(email: String)

  case class RecoverPasswordPayload(
      email: String,
      token: String,
      newPassword: String
  )
}

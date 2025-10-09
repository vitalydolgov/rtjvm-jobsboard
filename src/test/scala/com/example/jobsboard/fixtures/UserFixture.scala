package com.example.jobsboard.fixtures

import com.example.jobsboard.domain.user.*

trait UserFixture {
  val Admin = User(
    "admin@example.com",
    "$2a$10$MFtXdkP2q/wDZOBexuF8HuFYMiksRTwHnCDlmcVNvBAflhqqpsYR6", // passw0rd
    Some("Admin"),
    None,
    Some("Example.com"),
    Role.ADMIN
  )

  val adminEmail = Admin.email
  val adminPassword = "passw0rd"

  val NewAdmin = NewUserPayload(
    adminEmail,
    adminPassword,
    Some("Admin"),
    None,
    Some("Example.com")
  )

  val John = User(
    "john@acme.com",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("John"),
    Some("Smith"),
    Some("ACME Inc"),
    Role.RECRUITER
  )

  val johnEmail = John.email
  val johnPassword = "password"

  val JohnUpdated = User(
    "john@acme.com",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("John"),
    Some("Baker"),
    Some("ACME Inc"),
    Role.RECRUITER
  )

  val NewJohn = NewUserPayload(
    johnEmail,
    johnPassword,
    Some("John"),
    Some("Smith"),
    Some("ACME Inc")
  )

  val Carol = User(
    "carol@acme.com",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("Carol"),
    Some("Adams"),
    Some("ACME Inc"),
    Role.RECRUITER
  )

  val carolEmail = Carol.email
  val carolPassword = "password"

  val NewCarol = NewUserPayload(
    carolEmail,
    carolPassword,
    Some("Carol"),
    Some("Adams"),
    Some("ACME Inc")
  )

  val InvalidEmail = "nobody@example.com"
}

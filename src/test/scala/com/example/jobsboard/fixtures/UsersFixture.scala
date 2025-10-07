package com.example.jobsboard.fixtures

import com.example.jobsboard.domain.user.*

trait UsersFixture {
  val Admin = User(
    "admin@example.com",
    "$2a$10$MFtXdkP2q/wDZOBexuF8HuFYMiksRTwHnCDlmcVNvBAflhqqpsYR6", // passw0rd
    Some("Admin"),
    None,
    Some("Example.com"),
    Role.ADMIN
  )

  val John = User(
    "john@acme.com",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("John"),
    Some("Smith"),
    Some("ACME Inc"),
    Role.RECRUITER
  )

  val JohnUpdated = User(
    "john@acme.com",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("John"),
    Some("Baker"),
    Some("ACME Inc"),
    Role.RECRUITER
  )

  val Carol = User(
    "carol@acme.com",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("Carol"),
    Some("Adams"),
    Some("ACME Inc"),
    Role.RECRUITER
  )

  val InvalidEmail = "nobody@example.com"
}

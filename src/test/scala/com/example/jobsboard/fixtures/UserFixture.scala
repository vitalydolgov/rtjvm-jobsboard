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
    "jobs@encom.io",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("John"),
    Some("Smith"),
    Some("ENCOM"),
    Role.RECRUITER
  )

  val johnEmail = John.email
  val johnPassword = "password"

  val JohnUpdated = User(
    "jobs@encom.io",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("John"),
    Some("Baker"),
    Some("ENCOM"),
    Role.RECRUITER
  )

  val NewJohn = NewUserPayload(
    johnEmail,
    johnPassword,
    Some("John"),
    Some("Smith"),
    Some("ENCOM")
  )

  val Anna = User(
    "jobs@dillingersystems.io",
    "$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu", // password
    Some("Anna"),
    Some("Brown"),
    Some("Dillinger Systems"),
    Role.RECRUITER
  )

  val annaEmail = Anna.email
  val annaPassword = "password"

  val NewAnna = NewUserPayload(
    annaEmail,
    annaPassword,
    Some("Carol"),
    Some("Adams"),
    Some("Dillinger Systems")
  )

  val InvalidEmail = "nobody@example.com"
}

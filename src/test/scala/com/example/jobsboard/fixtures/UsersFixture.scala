package com.example.jobsboard.fixtures

import com.example.jobsboard.domain.user.*

trait UsersFixture {
  val Admin = User(
    "admin@example.com",
    "$dummyhashedpassword",
    Some("Admin"),
    None,
    Some("Example.com"),
    Role.ADMIN
  )

  val John = User(
    "john@acme.com",
    "$dummyhashedpassword",
    Some("John"),
    Some("Smith"),
    Some("ACME Inc"),
    Role.RECRUITER
  )

  val JohnUpdated = User(
    "john@acme.com",
    "$dummyhashedpassword",
    Some("John"),
    Some("Baker"),
    Some("ACME Inc"),
    Role.RECRUITER
  )

  val Carol = User(
    "carol@acme.com",
    "$dummyhashedpassword",
    Some("Carol"),
    Some("Adams"),
    Some("ACME Inc"),
    Role.RECRUITER
  )
}

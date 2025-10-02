package com.example.jobsboard.fixtures

import java.util.UUID
import com.example.jobsboard.domain.job._

trait JobFixture {
  val NewJobUuid = UUID.fromString("c2bd8078-6089-4270-813b-4f06b32c1fc8")

  val InvalidJobUuid = UUID.fromString("22e1971e-78ec-4669-baca-42953d93cec5")

  val ScalaDeveloperACME = Job(
    id = UUID.fromString("9283f9f6-65eb-4791-a92c-be32dba0a484"),
    date = 1735689600000L,
    ownerEmail = "hr@acme.com",
    jobInfo = JobInfo(
      company = "ACME Corp",
      title = "Senior Scala Developer",
      description =
        "We are looking for a skilled Scala developer to join our team and work on high-performance distributed systems.",
      externalUrl = "https://acme.com/careers/scala-developer",
      remote = false,
      location = "San Francisco, CA",
      salaryLo = None,
      salaryHi = None,
      currency = None,
      country = None,
      tags = Some(List("scala", "cats")),
      image = None,
      seniority = None,
      other = None
    ),
    active = false
  )

  val ScalaDeveloperACMEUpdated = Job(
    id = UUID.fromString("9283f9f6-65eb-4791-a92c-be32dba0a484"),
    date = 1735689600000L,
    ownerEmail = "hr@acme.com",
    jobInfo = JobInfo(
      company = "ACME Corp",
      title = "Lead Scala Developer",
      description =
        "We are looking for an experienced Scala developer to lead our team and architect high-performance distributed systems.",
      externalUrl = "https://acme.com/careers/lead-scala-developer",
      remote = false,
      location = "New York, NY",
      salaryLo = None,
      salaryHi = None,
      currency = None,
      country = None,
      tags = Some(List("scala", "cats")),
      image = None,
      seniority = None,
      other = None
    ),
    active = false
  )

  val JavaDeveloperTechCorp = Job(
    id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
    date = 1735689600000L,
    ownerEmail = "jobs@techcorp.com",
    jobInfo = JobInfo(
      company = "TechCorp",
      title = "Senior Java Developer",
      description =
        "Join our dynamic team as a Senior Java Developer to build scalable microservices and enterprise applications.",
      externalUrl = "https://techcorp.com/careers/java-developer",
      remote = true,
      location = "Austin, TX",
      salaryLo = None,
      salaryHi = None,
      currency = None,
      country = None,
      tags = None,
      image = None,
      seniority = None,
      other = None
    ),
    active = false
  )
}

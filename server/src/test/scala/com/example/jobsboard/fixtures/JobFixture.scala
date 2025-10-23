package com.example.jobsboard.fixtures

import java.util.UUID
import com.example.jobsboard.domain.job._

trait JobFixture {
  val NewJobUuid = UUID.fromString("c2bd8078-6089-4270-813b-4f06b32c1fc8")

  val InvalidJobUuid = UUID.fromString("22e1971e-78ec-4669-baca-42953d93cec5")

  val ScalaDeveloperENCOM = Job(
    id = UUID.fromString("9283f9f6-65eb-4791-a92c-be32dba0a484"),
    date = 1735689600000L,
    ownerEmail = "jobs@encom.io",
    jobInfo = JobInfo(
      company = "ENCOM",
      title = "Senior Scala Developer",
      description =
        "We are looking for a skilled Scala developer to join our team and work on high-performance distributed systems.",
      externalUrl = "https://encom.io/careers/scala-developer",
      remote = false,
      location = "San Francisco, CA",
      salaryLo = None,
      salaryHi = Some(100000),
      currency = None,
      country = Some("US"),
      tags = Some(List("scala", "cats")),
      image = None,
      seniority = Some("Senior"),
      other = None
    ),
    active = false
  )

  val ScalaDeveloperENCOMUpdated = Job(
    id = UUID.fromString("9283f9f6-65eb-4791-a92c-be32dba0a484"),
    date = 1735689600000L,
    ownerEmail = "jobs@encom.io",
    jobInfo = JobInfo(
      company = "ENCOM",
      title = "Lead Scala Developer",
      description =
        "We are looking for an experienced Scala developer to lead our team and architect high-performance distributed systems.",
      externalUrl = "https://encom.io/careers/lead-scala-developer",
      remote = false,
      location = "New York, NY",
      salaryLo = None,
      salaryHi = Some(100000),
      currency = None,
      country = Some("US"),
      tags = Some(List("scala", "cats")),
      image = None,
      seniority = Some("Senior"),
      other = None
    ),
    active = false
  )

  val ScalaDeveloperDillingerSystems = Job(
    id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
    date = 1735689600000L,
    ownerEmail = "jobs@dillingersystems.io",
    jobInfo = JobInfo(
      company = "Dillinger Systems",
      title = "Senior Scala Developer",
      description =
        "Join our dynamic team as a Senior Scala Developer to build scalable microservices and enterprise applications.",
      externalUrl = "https://dillingersystems.io/careers/scala-developer",
      remote = true,
      location = "Austin, TX",
      salaryLo = None,
      salaryHi = Some(100000),
      currency = None,
      country = Some("US"),
      tags = Some(List("scala", "cats")),
      image = None,
      seniority = Some("Senior"),
      other = None
    ),
    active = false
  )

  val DefaultFilter = JobFilter(
    companies = List("ENCOM")
  )
}

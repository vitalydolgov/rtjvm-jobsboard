package com.example.jobsboard.algebra

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import com.example.jobsboard.fixtures.*
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.domain.pagination.*

class JobsSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with JobFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val initScript: String = "sql/jobs.sql"

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          retrieved <- jobs.find(InvalidJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }

    "should retrieve job by ID" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          retrieved <- jobs.find(ScalaDeveloperENCOM.id)
        } yield retrieved

        program.asserting(_ shouldBe Some(ScalaDeveloperENCOM))
      }
    }

    "should retrieve all jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          retrieved <- jobs.all()
        } yield retrieved

        program.asserting(_ shouldBe List(ScalaDeveloperENCOM))
      }
    }

    "should create a new job, initially inactive" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          jobId <- jobs.create("jobs@dillingersystems.io", ScalaDeveloperDillingerSystems.jobInfo)
          job <- jobs.find(jobId)
        } yield job

        program.asserting(_.map(_.jobInfo) shouldBe None)
      }
    }

    "should activate a new job" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          jobId <- jobs.create("jobs@dillingersystems.io", ScalaDeveloperDillingerSystems.jobInfo)
          _ <- jobs.activate(jobId)
          job <- jobs.find(jobId)
        } yield job

        program.asserting(_.map(_.jobInfo) shouldBe Some(ScalaDeveloperDillingerSystems.jobInfo))
      }
    }

    "should return an updated job if it exists" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          jobOpt <- jobs.update(ScalaDeveloperENCOM.id, ScalaDeveloperENCOMUpdated.jobInfo)
        } yield jobOpt

        program.asserting(_ shouldBe Some(ScalaDeveloperENCOMUpdated))
      }
    }

    "should return None when updating a non-existent job" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          jobOpt <- jobs.update(InvalidJobUuid, ScalaDeveloperENCOMUpdated.jobInfo)
        } yield jobOpt

        program.asserting(_ shouldBe None)
      }
    }

    "should delete an existing job" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          deletedJobsCount <- jobs.delete(ScalaDeveloperENCOM.id)
          jobsCount <- sql"SELECT COUNT(*) FROM jobs WHERE id = ${ScalaDeveloperENCOM.id}"
            .query[Int]
            .unique
            .transact(xa)
        } yield (deletedJobsCount, jobsCount)

        program.asserting { case (deletedJobsCount, jobsCount) =>
          deletedJobsCount shouldBe 1
          jobsCount shouldBe 0
        }
      }
    }

    "should return zero when deleting a non-existent job" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          deletedJobsCount <- jobs.delete(InvalidJobUuid)
        } yield deletedJobsCount

        program.asserting(_ shouldBe 0)
      }
    }

    "should filter remote jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(remote = true), Pagination.default)
        } yield filteredJobs

        program.asserting(_ shouldBe List())
      }
    }

    "should filter jobs by tags" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(
            JobFilter(tags = List("scala", "cats", "zio")),
            Pagination.default
          )
        } yield filteredJobs

        program.asserting(_ shouldBe List(ScalaDeveloperENCOM))
      }
    }

    "should surface all possible filters" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filter <- jobs.possibleFilters()
        } yield filter

        program.asserting {
          case JobFilter(companies, locations, countries, seniorities, tags, maxSalary, remote) =>
            companies shouldBe List("ENCOM")
            locations shouldBe List("San Francisco, CA")
            countries shouldBe List("US")
            seniorities shouldBe List("Senior")
            tags.toSet shouldBe Set("cats", "scala")
            maxSalary shouldBe Some(100000)
            remote shouldBe false
        }
      }
    }
  }
}

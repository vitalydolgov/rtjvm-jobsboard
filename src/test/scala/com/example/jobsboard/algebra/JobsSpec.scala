package com.example.jobsboard.algebra

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import com.example.jobsboard.fixtures.*

class JobsSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with JobFixture {
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
  }

  "Jobs 'algebra'" - {
    "should retrieve job by UD" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          retrieved <- jobs.find(ScalaDeveloperACME.id)
        } yield retrieved

        program.asserting(_ shouldBe Some(ScalaDeveloperACME))
      }
    }

    "should retrieve all jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          retrieved <- jobs.all()
        } yield retrieved

        program.asserting(_ shouldBe List(ScalaDeveloperACME))
      }
    }

    "should create a new job" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          jobId <- jobs.create("jobs@techcorp.com", JavaDeveloperTechCorp.jobInfo)
          job <- jobs.find(jobId)
        } yield job

        program.asserting(_.map(_.jobInfo) shouldBe Some(JavaDeveloperTechCorp.jobInfo))
      }
    }

    "should return an updated job if it exists" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          jobOpt <- jobs.update(ScalaDeveloperACME.id, ScalaDeveloperACMEUpdated.jobInfo)
        } yield jobOpt

        program.asserting(_ shouldBe Some(ScalaDeveloperACMEUpdated))
      }
    }

    "should return None when updating non-existent job" in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          jobOpt <- jobs.update(InvalidJobUuid, ScalaDeveloperACMEUpdated.jobInfo)
        } yield jobOpt

        program.asserting(_ shouldBe None)
      }
    }
  }

  "should delete an existing job" in {
    transactor.use { xa =>
      val program = for {
        jobs <- LiveJobs[IO](xa)
        deletedJobsCount <- jobs.delete(ScalaDeveloperACME.id)
        jobsCount <- sql"SELECT COUNT(*) FROM jobs WHERE id = ${ScalaDeveloperACME.id}"
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

  "should return zero when deleting non-existent job" in {
    transactor.use { xa =>
      val program = for {
        jobs <- LiveJobs[IO](xa)
        deletedJobsCount <- jobs.delete(InvalidJobUuid)
      } yield deletedJobsCount

      program.asserting(_ shouldBe 0)
    }
  }
}

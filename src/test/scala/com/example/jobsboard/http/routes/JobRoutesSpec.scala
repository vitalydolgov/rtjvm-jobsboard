package com.example.jobsboard.http.routes

import cats.implicits.*
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import java.util.UUID
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import com.example.jobsboard.algebra.*
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.domain.pagination.*
import com.example.jobsboard.fixtures.JobFixture
import com.example.jobsboard.http.routes.JobRoutes

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture {

  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] = IO.pure(NewJobUuid)

    override def all(): IO[List[Job]] = IO.pure(List(ScalaDeveloperACME))

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if (filter.remote) IO.pure(List())
      else IO.pure(List(ScalaDeveloperACME))

    override def find(id: UUID): IO[Option[Job]] =
      if (id == ScalaDeveloperACME.id)
        IO.pure(Some(ScalaDeveloperACME))
      else
        IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == ScalaDeveloperACME.id)
        IO.pure(Some(ScalaDeveloperACMEUpdated))
      else
        IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if (id == ScalaDeveloperACME.id)
        IO.pure(1)
      else
        IO.pure(0)
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  "JobRoutes" - {
    "should return a job with a given ID" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs" / ScalaDeveloperACME.id.toString)
        )
        retrieved <- response.as[Job]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe ScalaDeveloperACME
      }
    }

    "should return all jobs" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter())
        )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(ScalaDeveloperACME)
      }
    }

    "should return all jobs that satisfy a filter" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter(remote = true))
        )
        retrieved <- response.as[List[Job]]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List()
      }
    }

    "should create a new job" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(ScalaDeveloperACME.jobInfo)
        )
        createdJobId <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        createdJobId shouldBe NewJobUuid
      }
    }

    "should only update a job that exists" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs" / ScalaDeveloperACME.id.toString)
            .withEntity(ScalaDeveloperACMEUpdated.jobInfo)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs" / InvalidJobUuid.toString)
            .withEntity(ScalaDeveloperACMEUpdated.jobInfo)
        )
      } yield {
        response.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should only delete a job that exists" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs" / ScalaDeveloperACME.id.toString)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs" / InvalidJobUuid.toString)
        )
      } yield {
        response.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }
}

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
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams

import com.example.jobsboard.algebra.*
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.domain.pagination.*
import com.example.jobsboard.fixtures.*
import com.example.jobsboard.http.routes.*

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture
    with SecuredRouteFixture {

  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] = IO.pure(NewJobUuid)

    override def all(): fs2.Stream[IO, Job] = fs2.Stream.emit(ScalaDeveloperENCOM)

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if (filter.remote) IO.pure(List())
      else IO.pure(List(ScalaDeveloperENCOM))

    override def find(id: UUID): IO[Option[Job]] =
      if (id == ScalaDeveloperENCOM.id)
        IO.pure(Some(ScalaDeveloperENCOM))
      else
        IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == ScalaDeveloperENCOM.id)
        IO.pure(Some(ScalaDeveloperENCOMUpdated))
      else
        IO.pure(None)

    override def activate(id: UUID): IO[Int] =
      IO.pure(1)

    override def delete(id: UUID): IO[Int] =
      if (id == ScalaDeveloperENCOM.id)
        IO.pure(1)
      else
        IO.pure(0)

    override def possibleFilters(): IO[JobFilter] = IO(DefaultFilter)
  }

  var stripe: Stripe[IO] = new Stripe[IO] {
    override def createCheckoutSession(jobId: String, userEmail: String): IO[Option[Session]] =
      IO.pure(Some(Session.create(SessionCreateParams.builder().build())))

    override def handleWebhook[A](
        payload: String,
        signature: String,
        action: String => IO[A]
    ): IO[Option[A]] = IO.pure(None)
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs, stripe).routes

  "JobRoutes" - {
    "should return a job with a given ID" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs" / ScalaDeveloperENCOM.id.toString)
        )
        retrieved <- response.as[Job]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe ScalaDeveloperENCOM
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
        retrieved shouldBe List(ScalaDeveloperENCOM)
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
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withBearerToken(jwtToken)
            .withEntity(ScalaDeveloperENCOM.jobInfo)
        )
        createdJobId <- response.as[UUID]
      } yield {
        response.status shouldBe Status.Created
        createdJobId shouldBe NewJobUuid
      }
    }

    "should only update a job that exists" in {
      for {
        jwtToken <- mockedAuthenticator.create(johnEmail)
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs" / ScalaDeveloperENCOM.id.toString)
            .withBearerToken(jwtToken)
            .withEntity(ScalaDeveloperENCOMUpdated.jobInfo)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs" / InvalidJobUuid.toString)
            .withBearerToken(jwtToken)
            .withEntity(ScalaDeveloperENCOMUpdated.jobInfo)
        )
      } yield {
        response.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should forbid the update of a job that the user doesn't own" in {
      for {
        jwtToken <- mockedAuthenticator.create(annaEmail)
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs" / ScalaDeveloperENCOM.id.toString)
            .withBearerToken(jwtToken)
            .withEntity(ScalaDeveloperENCOMUpdated.jobInfo)
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should only delete a job that exists" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs" / ScalaDeveloperENCOM.id.toString)
            .withBearerToken(jwtToken)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs" / InvalidJobUuid.toString)
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should surface all possible filters" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/filters")
        )
        retrieved <- response.as[JobFilter]
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe DefaultFilter
      }
    }
  }
}

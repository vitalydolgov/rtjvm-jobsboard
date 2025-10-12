package com.example.jobsboard.http.routes

import cats.implicits.*
import cats.effect.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*
import org.http4s.circe.CirceEntityCodec.*
import tsec.authentication.asAuthed
import org.typelevel.log4cats.Logger

import java.util.UUID
import scala.collection.mutable
import scala.language.implicitConversions

import com.example.jobsboard.algebra.*
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.http.responses.*
import com.example.jobsboard.logging.syntax.*
import com.example.jobsboard.http.validation.syntax.*
import com.example.jobsboard.domain.pagination.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.domain.user.*

class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F])
    extends HttpValidationDsl[F] {

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")

  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
      for {
        filter <- req.as[JobFilter]
        jobsList <- jobs.all(filter, Pagination(limit, offset))
        resp <- Ok(jobsList)
      } yield resp
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found."))
    }
  }

  private val createJobRoute: AuthRoute[F] = { case secreq @ POST -> Root / "create" asAuthed _ =>
    secreq.request.validate[JobInfo] { jobInfo =>
      for {
        jobId <- jobs.create("TODO@example.com", jobInfo)
        resp <- Created(jobId)
      } yield resp
    }
  }

  private val updateJobRoute: AuthRoute[F] = {
    case secreq @ PUT -> Root / UUIDVar(id) asAuthed user =>
      secreq.request.validate[JobInfo] { jobInfo =>
        jobs.find(id).flatMap {
          case None =>
            NotFound(FailureResponse(s"Cannot update: Job $id not found."))
          case Some(job) if user.owns(job) || user.isAdmin =>
            jobs.update(id, jobInfo) *> Ok()
          case _ =>
            Forbidden(FailureResponse("You can only update your own jobs."))
        }
      }
  }

  private val deleteJobRoute: AuthRoute[F] = { case DELETE -> Root / UUIDVar(id) asAuthed user =>
    jobs.find(id).flatMap {
      case None =>
        NotFound(FailureResponse(s"Cannot delete: Job $id not found."))
      case Some(job) if user.owns(job) || user.isAdmin =>
        jobs.delete(job.id) *> Ok()
      case _ =>
        Forbidden(FailureResponse("You can only delete your own jobs."))
    }
  }

  private val unauthedRoutes = allJobsRoute <+> findJobRoute
  private val authedRoutes = SecuredHandler[F].liftService(
    createJobRoute.restrictedTo(allRoles) |+|
      updateJobRoute.restrictedTo(allRoles) |+|
      deleteJobRoute.restrictedTo(allRoles)
  )

  val routes = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}

package com.example.jobsboard.http.routes

import io.circe.generic.auto.*
import cats.implicits.*
import cats.effect.Concurrent
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger

import java.util.UUID
import scala.collection.mutable

import com.example.jobsboard.algebra.*
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.http.responses.*
import com.example.jobsboard.logging.syntax.*

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends Http4sDsl[F] {

  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    for {
      jobsList <- jobs.all()
      resp <- Ok(jobsList)
    } yield resp
  }

  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found."))
    }
  }

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo].logError(e => s"Parsing failed: $e")
        jobId <- jobs.create("TODO@example.com", jobInfo)
        resp <- Created(jobId)
      } yield resp
  }

  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      for {
        jobInfo <- req.as[JobInfo]
        jobOption <- jobs.update(id, jobInfo)
        resp <- jobOption match {
          case Some(_) => Ok()
          case None    => NotFound(FailureResponse(s"Cannot update: Job $id not found."))
        }
      } yield resp
  }

  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ DELETE -> Root / UUIDVar(id) =>
      jobs.find(id).flatMap {
        case Some(job) =>
          for {
            _ <- jobs.delete(job.id)
            resp <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete: Job $id not found."))
      }
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}

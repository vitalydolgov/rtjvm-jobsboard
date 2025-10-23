package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import io.circe.parser.*
import io.circe.generic.auto.*
import cats.effect.IO

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.pages.Page.StatusKind

object JobListPage {
  trait Message extends App.Message
  case class SetErrorStatus(message: String) extends Message
  case class AddJobs(jobs: List[Job], canLoadMore: Boolean) extends Message
  case object LoadMore extends Message

  object Endpoints {
    def getJobs(limit: Int, offset: Int) = new Endpoint[Message] {
      override val location: String = Constants.endpoints.jobs + s"?limit=$limit&offset=$offset"
      override val method: Method = Method.Post

      override val onResponse: Response => Message = resp =>
        resp.status match {
          case Status(200, _) =>
            val rawJson = resp.body
            val parsedJson = parse(rawJson).flatMap(_.as[List[Job]])

            parsedJson match {
              case Left(err)   => SetErrorStatus(err.toString)
              case Right(jobs) => AddJobs(jobs, canLoadMore = offset == 0 || jobs.nonEmpty)
            }
          case Status(_, message) => SetErrorStatus(s"Error: $message")
        }

      override val onError: HttpError => Message = err => SetErrorStatus(err.toString)
    }
  }

  object Commands {
    def getJobs(
        filter: JobFilter = JobFilter(),
        limit: Int = Constants.defaultPageSize,
        offset: Int = 0
    ): Cmd[IO, Message] = Endpoints.getJobs(limit, offset).call(filter)
  }
}

final case class JobListPage(
    jobs: List[Job] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status("Loading", Page.StatusKind.LOADING))
) extends Page {

  import JobListPage.*

  override def initCommand: Cmd[IO, App.Message] = Commands.getJobs()

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = message match {
    case AddJobs(jobs, canLoadMore) =>
      (
        setSuccessStatus("Loaded").copy(
          jobs = this.jobs ++ jobs,
          canLoadMore = canLoadMore
        ),
        Cmd.None
      )
    case SetErrorStatus(message) => (setErrorStatus(message), Cmd.None)
    case LoadMore                => (this, Commands.getJobs(offset = jobs.length))
    case _                       => (this, Cmd.None)
  }

  private def jobCard(job: Job) = {
    div(`class` := "job-card")(
      div(`class` := "job-card-image")(
        img(
          `class` := "job-logo",
          src := job.jobInfo.image.getOrElse(""),
          alt := job.jobInfo.title
        )
      ),
      div(`class` := "job-card-content")(
        h4(s"${job.jobInfo.company} - ${job.jobInfo.title}")
      ),
      div(`class` := "job-card-apply")(
        a(href := job.jobInfo.externalUrl, target := "blank")("Apply")
      )
    )
  }

  private def loadMoreButtonOpt: Option[Html[App.Message]] = status.map { status =>
    div(`class` := "load-more-action")(
      status match {
        case Page.Status(_, Page.StatusKind.LOADING)     => div("Loading...")
        case Page.Status(message, Page.StatusKind.ERROR) => div(message)
        case Page.Status(_, Page.StatusKind.SUCCESS) =>
          if (canLoadMore)
            button(`type` := "button", onClick(LoadMore))("Load More")
          else
            div("All jobs loaded.")
      }
    )
  }

  override def view: Html[App.Message] =
    div(`class` := "jobs-container")(
      jobs.map(jobCard(_)) ++ loadMoreButtonOpt
    )
}

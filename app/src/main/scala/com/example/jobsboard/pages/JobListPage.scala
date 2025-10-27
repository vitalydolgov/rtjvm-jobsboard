package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import io.circe.generic.auto.*
import cats.effect.IO

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.components.*

object JobListPage {
  trait Message extends App.Message
  case class SetErrorStatus(message: String) extends Message
  case class AddJobs(jobs: List[Job], canLoadMore: Boolean) extends Message
  case object LoadMore extends Message
  case class ApplyFilters(filters: Map[String, Set[String]]) extends Message

  object Endpoints {
    def getJobs(limit: Int, offset: Int) = new Endpoint[Message] {
      override val location: String = Constants.endpoints.jobs + s"?limit=$limit&offset=$offset"
      override val method: Method = Method.Post

      override val onResponse: Response => Message =
        Endpoint.onResponse[List[Job], Message](
          jobs => AddJobs(jobs, canLoadMore = offset == 0 || jobs.nonEmpty),
          SetErrorStatus(_)
        )

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
    filterPanel: FilterPanel = FilterPanel(
      applyFilters = ApplyFilters(_)
    ),
    filter: JobFilter = JobFilter(),
    jobs: List[Job] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status.LOADING)
) extends Page {

  import JobListPage.*

  override def initCommand: Cmd[IO, App.Message] =
    filterPanel.initCommand |+| Commands.getJobs()

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  private def makeFilter(filters: Map[String, Set[String]]) = JobFilter(
    companies = filters.get("Companies").getOrElse(Set()).toList,
    locations = filters.get("Locations").getOrElse(Set()).toList,
    countries = filters.get("Countries").getOrElse(Set()).toList,
    seniorities = filters.get("Seniorities").getOrElse(Set()).toList,
    tags = filters.get("Tags").getOrElse(Set()).toList,
    maxSalary = Some(filterPanel.maxSalary).filter(_ > 0),
    remote = filterPanel.remote
  )

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
    case ApplyFilters(filters) =>
      val newFilter = makeFilter(filters)
      (this.copy(jobs = List(), filter = newFilter), Commands.getJobs(newFilter))
    case message: FilterPanel.Message =>
      val (newFilterPanel, command) = filterPanel.update(message)
      (this.copy(filterPanel = newFilterPanel), command)
    case _ => (this, Cmd.None)
  }

  private def loadMoreButtonOpt: Option[Html[App.Message]] = status.map { status =>
    div(`class` := "load-more-action")(
      status match {
        case Page.Status(_, Page.StatusKind.LOADING)     => div("Loading...")
        case Page.Status(message, Page.StatusKind.ERROR) => div(message)
        case Page.Status(_, Page.StatusKind.SUCCESS) =>
          if (canLoadMore)
            button(`type` := "button", `class` := "load-more-button", onClick(LoadMore))(
              "Load More"
            )
          else
            div("All jobs loaded.")
      }
    )
  }

  override def view: Html[App.Message] =
    section(`class` := "section-1")(
      div(`class` := "container")(
        div(`class` := "row jvm-recent-jobs-body")(
          div(`class` := "col-lg-4")(
            filterPanel.view
          ),
          div(`class` := "col-lg-8")(
            jobs.map(JobComponents.card) ++ loadMoreButtonOpt
          )
        )
      )
    )
}

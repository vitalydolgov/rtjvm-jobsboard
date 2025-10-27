package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO
import io.circe.generic.auto.*
import laika.api.*
import laika.format.*

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.components.*
import com.example.jobsboard.domain.job.*

object JobPage {
  trait Message extends App.Message
  case class SetJob(job: Job) extends Message
  case class SetError(message: String) extends Message

  object Endpoints {
    def getJob(id: String) = new Endpoint[Message] {
      override val location: String = Constants.endpoints.jobs + s"/$id"
      override val method: Method = Method.Get

      override val onResponse: Response => Message =
        Endpoint.onResponse[Job, Message](SetJob(_), SetError(_))

      override val onError: HttpError => Message =
        err => SetError(err.toString)
    }
  }

  object Commands {
    def getJob(id: String): Cmd[IO, Message] =
      Endpoints.getJob(id).call()
  }
}

final case class JobPage(
    id: String,
    jobOpt: Option[Job] = None,
    status: Page.Status = Page.Status.LOADING
) extends Page {

  import JobPage.*

  override def initCommand: Cmd[IO, Message] = Commands.getJob(id)

  private def setErrorStatus(message: String) =
    this.copy(status = Page.Status(message, Page.StatusKind.ERROR))

  private def setSuccessStatus(message: String) =
    this.copy(status = Page.Status(message, Page.StatusKind.SUCCESS))

  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = message match {
    case SetJob(job)       => (setSuccessStatus("Success").copy(jobOpt = Some(job)), Cmd.None)
    case SetError(message) => (setErrorStatus(message), Cmd.None)
    case _                 => (this, Cmd.None)
  }

  private def markdownTransformer = Transformer
    .from(Markdown)
    .to(HTML)
    .build

  private def jobDescription(job: Job) = {
    val descriptionHtml = markdownTransformer.transform(job.jobInfo.description) match {
      case Left(err)   => "Failed to render markdown for this job description."
      case Right(html) => html
    }
    div(`class` := "job-description")().innerHtml(descriptionHtml)
  }

  private def jobPage(job: Job) =
    div(`class` := "job-page")(
      div(`class` := "job-hero")(
        img(
          `class` := "job-logo",
          src := job.jobInfo.image.getOrElse(""),
          alt := job.jobInfo.title
        ),
        h1(s"${job.jobInfo.company} - ${job.jobInfo.title}")
      ),
      div(`class` := "job-overview")(
        JobComponents.summary(job)
      ),
      jobDescription(job),
      a(
        href := job.jobInfo.externalUrl,
        `class` := "job-apply-action",
        target := "blank"
      )("Apply")
    )

  private def noJobPage = status.kind match {
    case Page.StatusKind.LOADING => div("Loading...")
    case Page.StatusKind.ERROR   => div("Ouch! This job doesn't exist.")
    case Page.StatusKind.SUCCESS => div()
  }

  override def view: Html[App.Message] = jobOpt match {
    case Some(job) => jobPage(job)
    case None      => noJobPage
  }
}

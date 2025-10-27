package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO
import io.circe.generic.auto.*
import laika.api.*
import laika.format.*
import scala.scalajs.*
import scala.scalajs.js.*
import scala.scalajs.js.annotation.*

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.components.*
import com.example.jobsboard.domain.job.*

@js.native
@JSGlobal()
class Moment extends js.Object {
  def fromNow(): String = js.native
}

@js.native
@JSImport("moment", JSImport.Default)
object MomentLib extends js.Object {
  def unix(date: Long): Moment = js.native
}

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
    div(`class` := "container-fluid the-rock")(
      div(`class` := "row jvm-jobs-details-top-card")(
        div(`class` := "col-md-12 p-0")(
          div(`class` := "jvm-jobs-details-card-profile-img")(
            img(
              `class` := "img-fluid",
              src := job.jobInfo.image.getOrElse(""),
              alt := job.jobInfo.title
            )
          ),
          div(`class` := "jvm-jobs-details-card-profile-title")(
            h1(s"${job.jobInfo.company} - ${job.jobInfo.title}"),
            div(`class` := "jvm-jobs-details-card-profile-job-details-company-and-location")(
              JobComponents.summary(job)
            )
          ),
          div(`class` := "jvm-jobs-details-card-apply-now-btn")(
            a(href := job.jobInfo.externalUrl, target := "blank")(
              button(`type` := "button", `class` := "btn btn-warning")("Apply now")
            ),
            p(MomentLib.unix(job.date / 1000).fromNow())
          )
        )
      ),
      div(`class` := "container-fluid")(
        div(`class` := "container")(
          div(`class` := "markdown-body overview-section")(
            jobDescription(job)
          )
        ),
        div(`class` := "container")(
          div(`class` := "rok-last")(
            div(`class` := "row")(
              div(`class` := "col-md-6 col-sm-6 col-6")(
                span(`class` := "rock-apply")("Apply for this job.")
              ),
              div(`class` := "col-md-6 col-sm-6 col-6")(
                a(href := job.jobInfo.externalUrl, target := "blank")(
                  button(`type` := "button", `class` := "rock-apply-btn")("Apply now")
                )
              )
            )
          )
        )
      )
    )

  private def noJobPage =
    div(`class` := "container-fluid the-rock")(
      div(`class` := "row jvm-jobs-details-top-card")(
        status.kind match {
          case Page.StatusKind.LOADING => h1("Loading...")
          case Page.StatusKind.ERROR   => h1("Ouch! This job doesn't exist.")
          case Page.StatusKind.SUCCESS => h1("Unknown error.")
        }
      )
    )

  override def view: Html[App.Message] = jobOpt match {
    case Some(job) => jobPage(job)
    case None      => noJobPage
  }
}

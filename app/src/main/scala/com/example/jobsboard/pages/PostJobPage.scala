package com.example.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import tyrian.cmds.Logger
import cats.implicits.*
import cats.effect.IO
import io.circe.parser.*
import io.circe.generic.auto.*
import org.scalajs.dom.{File, FileReader}
import scala.util.Try

import com.example.jobsboard.*
import com.example.jobsboard.core.*
import com.example.jobsboard.common.*
import com.example.jobsboard.domain.job.*

object PostJobPage {
  trait Message extends App.Message
  case class UpdateCompany(company: String) extends Message
  case class UpdateTitle(title: String) extends Message
  case class UpdateDescription(description: String) extends Message
  case class UpdateExternalUrl(externalUrl: String) extends Message
  case object ToggleRemote extends Message
  case class UpdateLocation(location: String) extends Message
  case class UpdateSalaryLo(salaryLo: Int) extends Message
  case class UpdateSalaryHi(salaryHi: Int) extends Message
  case class UpdateCurrency(currency: String) extends Message
  case class UpdateCountry(country: String) extends Message
  case class UpdateTags(tags: String) extends Message
  case class UpdateImageFile(fileOpt: Option[File]) extends Message
  case class UpdateImage(imageOpt: Option[String]) extends Message
  case class UpdateSeniority(seniority: String) extends Message
  case class UpdateOther(other: String) extends Message
  case object AttemptPostJob extends Message
  case class PostJobError(message: String) extends Message
  case class PostJobSuccess(jobId: String) extends Message

  object Endpoints {
    val postJob = new Endpoint[Message] {
      override val location: String = Constants.endpoints.postJob
      override val method: Method = Method.Post

      override val onResponse: Response => Message = resp =>
        resp.status match {
          case Status(code, _) if code >= 200 && code < 300 =>
            val jobId = resp.body
            PostJobSuccess(jobId)
          case Status(401, _) =>
            PostJobError("You're unauthorized, please log in.")
          case Status(code, _) if code >= 400 && code < 500 =>
            val rawJson = resp.body
            val parsedJson = parse(rawJson).flatMap(_.hcursor.get[String]("error"))
            parsedJson match {
              case Left(error)    => PostJobError(s"Error: ${error.getMessage}")
              case Right(message) => PostJobError(message)
            }
          case _ => PostJobError("Unknown error.")
        }

      override val onError: HttpError => Message =
        err => PostJobError(err.toString)
    }
  }

  object Commands {
    def postJob(
        company: String,
        title: String,
        description: String,
        externalUrl: String,
        remote: Boolean,
        location: String,
        salaryLo: Option[Int],
        salaryHi: Option[Int],
        currency: Option[String],
        country: Option[String],
        tags: Option[String],
        image: Option[String],
        seniority: Option[String],
        other: Option[String]
    ): Cmd[IO, Message] = {
      Endpoints.postJob.callAuthorized(
        JobInfo(
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags.map(text => text.split(",").map(_.trim).toList),
          image,
          seniority,
          other
        )
      )
    }

    def loadFile(fileOpt: Option[File]): Cmd[IO, Message] =
      Cmd.Run[IO, Option[String], Message] {
        fileOpt.traverse { file =>
          IO.async_ { cb =>
            val reader = new FileReader
            reader.onload = _ => cb(Right(reader.result.toString))
            reader.readAsDataURL(file)
          }
        }
      }(UpdateImage(_))
  }
}

final case class PostJobPage(
    company: String = "",
    title: String = "",
    description: String = "",
    externalUrl: String = "",
    remote: Boolean = false,
    location: String = "",
    salaryLo: Option[Int] = None,
    salaryHi: Option[Int] = None,
    currency: Option[String] = None,
    country: Option[String] = None,
    tags: Option[String] = None,
    image: Option[String] = None,
    seniority: Option[String] = None,
    other: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post a Job", status) {

  import PostJobPage.*

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  override def update(message: App.Message): (Page, Cmd[IO, App.Message]) = message match {
    case UpdateCompany(company)         => (this.copy(company = company), Cmd.None)
    case UpdateTitle(title)             => (this.copy(title = title), Cmd.None)
    case UpdateDescription(description) => (this.copy(description = description), Cmd.None)
    case UpdateExternalUrl(externalUrl) => (this.copy(externalUrl = externalUrl), Cmd.None)
    case ToggleRemote                   => (this.copy(remote = !remote), Cmd.None)
    case UpdateLocation(location)       => (this.copy(location = location), Cmd.None)
    case UpdateSalaryLo(salaryLo)       => (this.copy(salaryLo = Some(salaryLo)), Cmd.None)
    case UpdateSalaryHi(salaryHi)       => (this.copy(salaryHi = Some(salaryHi)), Cmd.None)
    case UpdateCurrency(currency)       => (this.copy(currency = Some(currency)), Cmd.None)
    case UpdateCountry(country)         => (this.copy(country = Some(country)), Cmd.None)
    case UpdateTags(tags)               => (this.copy(tags = Some(tags)), Cmd.None)
    case UpdateImageFile(file)          => (this, Commands.loadFile(file))
    case UpdateImage(image)             => (this.copy(image = image), Cmd.None)
    case UpdateSeniority(seniority)     => (this.copy(seniority = Some(seniority)), Cmd.None)
    case UpdateOther(other)             => (this.copy(other = Some(other)), Cmd.None)
    case AttemptPostJob =>
      (
        this,
        Commands.postJob(
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags,
          image,
          seniority,
          other
        )
      )
    case PostJobError(message) => (setErrorStatus(message), Cmd.None)
    case PostJobSuccess(jobId) => (this, Logger.consoleLog(s"jobs/$jobId"))
    case _                     => (this, Cmd.None)
  }

  private def parseNumber(string: String) =
    Try(string.toInt).getOrElse(0)

  override def content: List[Html[App.Message]] = List(
    formInput("Company", "company", "text", true, UpdateCompany(_)),
    formInput("Title", "title", "text", true, UpdateTitle(_)),
    formTextArea("Description", "description", true, UpdateDescription(_)),
    formInput("URL", "external-url", "text", true, UpdateExternalUrl(_)),
    formInput("Remote", "remote", "checkbox", false, _ => ToggleRemote),
    formInput("Location", "location", "text", true, UpdateLocation(_)),
    formInput("Salary Lo", "salary-lo", "number", false, str => UpdateSalaryLo(parseNumber(str))),
    formInput("Salary Hi", "salary-hi", "number", false, str => UpdateSalaryHi(parseNumber(str))),
    formInput("Currency", "currency", "text", false, UpdateCurrency(_)),
    formInput("Country", "country", "text", false, UpdateCountry(_)),
    formInput("Tags", "tags", "text", false, UpdateTags(_)),
    formImageUpload("Logo", "logo", image, UpdateImageFile(_)),
    formInput("Seniority", "seniority", "text", false, UpdateSeniority(_)),
    formInput("Other", "other", "text", false, UpdateOther(_)),
    button(`type` := "button", onClick(AttemptPostJob))("Post Job")
  )

  private def notLoggedInView: Html[App.Message] =
    div("You're not logged in yet.")

  override def view: Html[App.Message] =
    if (Session.isActive) super.view
    else notLoggedInView
}

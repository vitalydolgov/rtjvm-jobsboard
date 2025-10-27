package com.example.jobsboard.components

import tyrian.*
import tyrian.Html.*

import com.example.jobsboard.*
import com.example.jobsboard.pages.*
import com.example.jobsboard.domain.job.*

object JobComponents {
  private def detail(icon: String, value: String) =
    div(`class` := "job-detail")(
      i(`class` := s"fa fa-$icon job-detail-icon")(),
      p(`class` := "job-detail-value")(value)
    )

  private def detailOpt(icon: String, valueOpt: Option[String]) =
    valueOpt.map(detail(icon, _)).getOrElse(div())

  private def salary(job: Job) = {
    val currency = job.jobInfo.currency.getOrElse("")

    (job.jobInfo.salaryLo, job.jobInfo.salaryHi) match {
      case (Some(salaryLo), Some(salaryHi)) => s"$currency $salaryLo-$salaryHi"
      case (Some(salaryLo), None)           => s"more than $currency $salaryLo"
      case (None, Some(salaryHi))           => s"up to $currency $salaryHi"
      case (None, None)                     => "unspecified"
    }
  }

  private def location(job: Job) = job.jobInfo.country match {
    case Some(country) => s"${job.jobInfo.location}, ${country}"
    case None          => job.jobInfo.location
  }

  def summary(job: Job) =
    div(`class` := "job-summary")(
      detail("dollar", salary(job)),
      detail("location-dot", location(job)),
      detailOpt("ranking-star", job.jobInfo.seniority),
      detailOpt("tags", job.jobInfo.tags.map(_.mkString(", ")))
    )

  def card(job: Job): Html[App.Message] = {
    div(`class` := "jvm-recent-jobs-cards")(
      div(`class` := "jvm-recent-jobs-card-img")(
        img(
          `class` := "img-fluid",
          src := job.jobInfo.image.getOrElse(""),
          alt := job.jobInfo.title
        )
      ),
      div(`class` := "jvm-recent-jobs-card-contents")(
        h5(
          Anchors.simpleNavLink(
            s"${job.jobInfo.company} - ${job.jobInfo.title}",
            Page.Urls.JOB(job.id.toString),
            "job-title-link"
          )
        ),
        summary(job)
      ),
      div(`class` := "jvm-recent-jobs-card-btn-apply")(
        a(href := job.jobInfo.externalUrl, target := "blank")(
          button(`class` := "btn btn-danger", `type` := "button")("Apply")
        )
      )
    )
  }
}

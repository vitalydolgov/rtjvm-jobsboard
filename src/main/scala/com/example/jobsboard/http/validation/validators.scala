package com.example.jobsboard.http.validation

import cats.*
import cats.implicits.*
import cats.data.*
import cats.data.Validated.*

import com.example.jobsboard.domain.job.*
import org.http4s.Uri
import java.net.URL
import scala.util.Try
import scala.util.Success
import scala.util.Failure

object validators {
  sealed trait ValidationFalure(val errorMessage: String)
  case class EmptyField(fieldName: String) extends ValidationFalure(s"'$fieldName' is required")
  case class InvalidUrl(fieldName: String)
      extends ValidationFalure(s"'$fieldName' is an invalid URL")

  type ValidationResult[A] = ValidatedNel[ValidationFalure, A]

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  def validateRequired[A](value: A, fieldName: String)(
      required: A => Boolean
  ): ValidationResult[A] =
    if (required(value)) value.validNel
    else EmptyField(fieldName).invalidNel

  def validateUrl(value: String, fieldName: String): ValidationResult[String] =
    Try(URL(value).toURI()) match {
      case Success(_)         => value.validNel
      case Failure(exception) => InvalidUrl(fieldName).invalidNel
    }

  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
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
    ) = jobInfo

    val validCompany = validateRequired(company, "company")(_.nonEmpty)
    val validTitle = validateRequired(title, "title")(_.nonEmpty)
    val validDescription = validateRequired(description, "description")(_.nonEmpty)
    val validExternalUrl = validateUrl(externalUrl, "externalUrl")
    val validLocation = validateRequired(location, "location")(_.nonEmpty)

    (
      validCompany,
      validTitle,
      validDescription,
      validExternalUrl,
      remote.validNel,
      validLocation,
      salaryLo.validNel,
      salaryHi.validNel,
      currency.validNel,
      country.validNel,
      tags.validNel,
      image.validNel,
      seniority.validNel,
      other.validNel
    ).mapN(JobInfo.apply)
  }
}

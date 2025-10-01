package com.example.jobsboard.http.validation

import cats.*
import cats.implicits.*
import cats.data.Validated.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger

import validators.*
import com.example.jobsboard.logging.syntax.*
import com.example.jobsboard.http.responses.*

object syntax {
  def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_]: MonadThrow: Logger] extends Http4sDsl[F] {
    extension (req: Request[F])
      def validate[A: Validator](ifValid: A => F[Response[F]])(using
          EntityDecoder[F, A]
      ): F[Response[F]] =
        req
          .as[A]
          .logError(e => s"Parsing payload failed: $e")
          .map(validateEntity)
          .flatMap {
            case Valid(entity) => ifValid(entity)
            case Invalid(errors) =>
              BadRequest(FailureResponse(errors.toList.map(_.errorMessage).mkString(", ")))
          }
  }
}

package com.example.jobsboard.http.routes

import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.server.*
import org.http4s.circe.CirceEntityCodec.*
import tsec.authentication.{SecuredRequestHandler, asAuthed, TSecAuthService}
import org.typelevel.log4cats.Logger

import scala.language.implicitConversions

import com.example.jobsboard.algebra.*
import com.example.jobsboard.http.validation.syntax.*
import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.user.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.http.responses.*

class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F] {
  private val authenticator = auth.authenticator

  private val securedHandler: SecuredRequestHandler[F, String, User, JwtToken] =
    SecuredRequestHandler(authenticator)

  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.validate[LoginPayload] { payload =>
      val jwtTokenOpt = for {
        tokenOpt <- auth.login(payload.email, payload.password)
      } yield tokenOpt

      jwtTokenOpt.map {
        case Some(jwtToken) => authenticator.embed(Response[F](Status.Ok), jwtToken)
        case None           => Response[F](Status.Unauthorized)
      }
    }
  }

  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req.validate[NewUserPayload] { payload =>
        for {
          userOpt <- auth.signUp(payload)
          response <- userOpt match {
            case Some(user) => Created(user.email)
            case None       => BadRequest(s"User with email ${payload.email} already exists.")
          }
        } yield response
      }
  }

  private val changePasswordRoute: AuthRoute[F] = {
    case req @ PUT -> Root / "users" / "password" asAuthed user =>
      req.request.validate[NewPasswordPayload] { payload =>
        for {
          userOptOrErr <- auth.changePassword(user.email, payload)
          response <- userOptOrErr match {
            case Right(Some(_)) => Ok()
            case Right(None)    => NotFound(FailureResponse(s"User ${user.email} not found."))
            case Left(_)        => Forbidden()
          }
        } yield response
      }
  }

  private val logoutRoute: AuthRoute[F] = { case req @ POST -> Root / "logout" asAuthed _ =>
    val token = req.authenticator
    for {
      _ <- authenticator.discard(token)
      resp <- Ok()
    } yield resp
  }

  private val deleteUserRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / "users" / email asAuthed _ =>
      auth.delete(email).flatMap {
        case true  => Ok()
        case false => NotFound()
      }
  }

  private val unauthedRoutes = loginRoute <+> createUserRoute
  private val authedRoutes =
    securedHandler.liftService(
      changePasswordRoute.restrictedTo(allRoles) |+|
        logoutRoute.restrictedTo(allRoles) |+|
        deleteUserRoute.restrictedTo(adminOnly)
    )

  val routes = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) =
    new AuthRoutes[F](auth)
}

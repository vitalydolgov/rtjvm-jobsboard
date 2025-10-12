package com.example.jobsboard.http.routes

import cats.data.*
import cats.effect.*
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.typelevel.ci.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import com.example.jobsboard.algebra.*
import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.domain.user.*
import com.example.jobsboard.http.routes.*
import com.example.jobsboard.fixtures.*

class AuthRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with SecuredRouteFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedAuth = new Auth[IO] {
    override def login(email: String, password: String): IO[Option[User]] =
      if (email == adminEmail && password == adminPassword)
        Some(Admin).pure[IO]
      else
        None.pure[IO]

    override def signUp(payload: NewUserPayload): IO[Option[User]] =
      if (payload.email == johnEmail)
        Some(John).pure[IO]
      else
        None.pure[IO]

    override def changePassword(
        email: String,
        payload: NewPasswordPayload
    ): IO[Either[String, Option[User]]] =
      if (email == adminEmail)
        if (payload.oldPassword == adminPassword)
          Right(Some(Admin)).pure[IO]
        else
          Left("Invalid password").pure[IO]
      else
        Right(None).pure[IO]

    override def delete(email: String): IO[Boolean] = true.pure[IO]

  }

  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

  "AuthRoutes" - {
    "should return 401 Unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginPayload(adminEmail, "wrongpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return 200 OK and JWT if login is successful" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginPayload(adminEmail, adminPassword))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return 400 Bad Request if the user to create already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewAdmin)
        )
      } yield {
        response.status shouldBe Status.BadRequest
      }
    }

    "should return 201 Created if sign up succeeds" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewJohn)
        )
      } yield {
        response.status shouldBe Status.Created
      }
    }

    "should return 401 Unauthorized if logging out without a JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return 200 OK if logging out with a valid JWT" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return 404 Not Found if changing password for non-existent user" in {
      for {
        jwtToken <- mockedAuthenticator.create(johnEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordPayload(johnPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.NotFound
      }
    }

    "should return 403 Forbidden if old password is incorrect" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordPayload("wrongpassword", "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }

    "should return 401 Unauthorized if changing password without JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordPayload(adminPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return 200 OK if changing password with a valid JWT" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordPayload(adminPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return 401 Unauthorized if non-admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(johnEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/admin@example.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return 200 OK if admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(adminEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/admin@example.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }
  }
}

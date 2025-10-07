package com.example.jobsboard.algebra

import cats.data.*
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.mac.jca.HMACSHA256
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import scala.concurrent.duration.*

import com.example.jobsboard.domain.user.*
import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.fixtures.*

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UsersFixture {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == John.email) IO.pure(Some(John))
      else IO.pure(None)

    override def create(user: User): IO[String] = IO.pure(user.email)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }

  val mockedAuthenticator: Authenticator[IO] = {
    val key = HMACSHA256.unsafeGenerateKey

    val identityStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == John.email) OptionT.pure(John)
      else if (email == Carol.email) OptionT.pure(Carol)
      else OptionT.none[IO, User]

    JWTAuthenticator.unbacked.inBearerToken(
      1.day,
      None,
      identityStore,
      key
    )
  }

  "Auth 'algebra'" - {
    "login should return None if the user doesn't exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        authOpt <- auth.login(InvalidEmail, "password")
      } yield authOpt

      program.asserting(_ shouldBe None)
    }

    "login should return None if the password is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        authOpt <- auth.login(John.email, "wrongpassword")
      } yield authOpt

      program.asserting(_ shouldBe None)
    }

    "login should return token on successful authentication" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        authOpt <- auth.login(John.email, "password")
      } yield authOpt

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        userOpt <- auth.signUp(NewUserInfo(John.email, "password", None, None, None))
      } yield userOpt

      program.asserting(_ shouldBe None)
    }

    "signing up should create a new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        userOpt <- auth.signUp(
          NewUserInfo("bob@acme.com", "password", Some("Bob"), Some("Jones"), Some("ACME Inc"))
        )
      } yield userOpt

      program.asserting {
        case Some(user) =>
          user.email shouldBe "bob@acme.com"
          user.firstName shouldBe Some("Bob")
          user.lastName shouldBe Some("Jones")
          user.company shouldBe Some("ACME Inc")
          user.role shouldBe Role.RECRUITER
        case _ => fail()
      }
    }

    "changing password should return Right(None) if the user doesn't exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword(InvalidEmail, NewPasswordInfo("oldpassword", "newpassword"))
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "changing password should return Left with an error if the password is incorrect" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword(John.email, NewPasswordInfo("oldpassword", "newpassword"))
      } yield result

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "changing password should succeed" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword(John.email, NewPasswordInfo("password", "newpassword"))
        isCorrectPassword <- result match {
          case Right(Some(user)) =>
            BCrypt.checkpwBool[IO](
              "newpassword",
              PasswordHash[BCrypt](user.hashedPassword)
            )
          case _ => IO.pure(false)
        }
      } yield isCorrectPassword

      program.asserting(_ shouldBe true)
    }
  }
}

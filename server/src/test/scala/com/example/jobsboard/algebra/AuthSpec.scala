package com.example.jobsboard.algebra

import cats.data.*
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import scala.concurrent.duration.*

import com.example.jobsboard.domain.user.*
import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.fixtures.*
import com.example.jobsboard.config.*

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val mockedSecurityConfig = SecurityConfig("secret", 1.day)

  val mockedTokens: Tokens[IO] = new Tokens[IO] {
    override def getToken(email: String): IO[Option[String]] =
      if (email == adminEmail) IO.pure(Some("ABCDEFGH"))
      else IO.pure(None)

    override def checkToken(email: String, token: String): IO[Boolean] =
      IO.pure(email == adminEmail && token == "ABCDEFGH")
  }

  val mockedEmails: Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
      IO.unit

    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] =
      IO.unit
  }

  private def probedEmails(users: Ref[IO, Set[String]]): Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
      users.modify(set => (set + to, ()))

    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] =
      sendEmail(to, "Your recovery token", "ABCDEFGH")
  }

  "Auth 'algebra'" - {
    "login should return None if the user doesn't exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        authOpt <- auth.login(InvalidEmail, "password")
      } yield authOpt

      program.asserting(_ shouldBe None)
    }

    "login should return None if the password is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        authOpt <- auth.login(adminEmail, "wrongpassword")
      } yield authOpt

      program.asserting(_ shouldBe None)
    }

    "login should return token on successful authentication" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        authOpt <- auth.login(adminEmail, adminPassword)
      } yield authOpt

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        userOpt <- auth.signUp(NewUserPayload(adminEmail, adminPassword, None, None, None))
      } yield userOpt

      program.asserting(_ shouldBe None)
    }

    "signing up should create a new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        userOpt <- auth.signUp(
          NewUserPayload("bob@acme.com", "password", Some("Bob"), Some("Jones"), Some("ACME Inc"))
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
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(
          InvalidEmail,
          NewPasswordPayload("oldpassword", "newpassword")
        )
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "changing password should return Left with an error if the password is incorrect" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(adminEmail, NewPasswordPayload("oldpassword", "newpassword"))
      } yield result

      program.asserting(_ shouldBe Left("Invalid password"))
    }

    "changing password should succeed" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(adminEmail, NewPasswordPayload(adminPassword, "newpassword"))
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

    "recover password should fail for a non-existent user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        resultValidToken <- auth.recoverPassword(InvalidEmail, "ABCDEFGH", "newpassword")
        resultInvalidToken <- auth.recoverPassword(InvalidEmail, "INVALID", "newpassword")
      } yield (resultValidToken, resultInvalidToken)

      program.asserting { case (resultValidToken, resultInvalidToken) =>
        resultValidToken shouldBe false
        resultInvalidToken shouldBe false
      }
    }

    "recover password should fail if the user exists but the token is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPassword(adminEmail, "INVALID", "newpassword")
      } yield result

      program.asserting(_ shouldBe false)
    }

    "recover password should succeed for the correct combination of user and token" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPassword(adminEmail, "ABCDEFGH", "newpassword")
      } yield result

      program.asserting(_ shouldBe true)
    }

    "sending recovery password should fail for a non-existent user" in {
      val program = for {
        set <- Ref.of[IO, Set[String]](Set())
        emails <- IO(probedEmails(set))
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result <- auth.sendPasswordRecoveryToken(InvalidEmail)
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ shouldBe empty)
    }

    "sending recovery password should succeed for user that exists" in {
      val program = for {
        set <- Ref.of[IO, Set[String]](Set())
        emails <- IO(probedEmails(set))
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result <- auth.sendPasswordRecoveryToken(adminEmail)
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ should contain(adminEmail))
    }
  }
}

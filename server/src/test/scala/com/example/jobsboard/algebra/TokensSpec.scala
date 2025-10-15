package com.example.jobsboard.algebra

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

import com.example.jobsboard.fixtures.*
import com.example.jobsboard.config.*

class TokensSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with UserFixture {

  val initScript: String = "sql/recovery_tokens.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Tokens 'algebra'" - {
    "should not create a new token for non-existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(1000000L))
          token <- tokens.getToken(InvalidEmail)
        } yield token

        program.asserting(_ shouldBe None)
      }
    }

    "should create a token for existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(1000000L))
          token <- tokens.getToken(adminEmail)
        } yield token

        program.asserting(_ shouldBe defined)
      }
    }

    "should not validate an expired token" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100L))
          tokenOpt <- tokens.getToken(adminEmail)
          _ <- IO.sleep(500.millis)
          isValid <- tokenOpt match {
            case Some(token) => tokens.checkToken(adminEmail, token)
            case None        => IO.pure(false)
          }
        } yield isValid

        program.asserting(_ shouldBe false)
      }
    }

    "should validate a non-expired token" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(1000000L))
          tokenOpt <- tokens.getToken(adminEmail)
          isValid <- tokenOpt match {
            case Some(token) => tokens.checkToken(adminEmail, token)
            case None        => IO.pure(false)
          }
        } yield isValid

        program.asserting(_ shouldBe true)
      }
    }

    "should not validate an unowned token" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(1000000L))
          tokenOpt <- tokens.getToken(adminEmail)
          isValidOwned <- tokenOpt match {
            case Some(token) => tokens.checkToken(adminEmail, token)
            case None        => IO.pure(false)
          }
          isValidUnowned <- tokenOpt match {
            case Some(token) => tokens.checkToken(InvalidEmail, token)
            case None        => IO.pure(false)
          }
        } yield (isValidOwned, isValidUnowned)

        program.asserting { case (isValidOwned, isValidUnowned) =>
          isValidOwned shouldBe true
          isValidUnowned shouldBe false
        }
      }
    }
  }
}

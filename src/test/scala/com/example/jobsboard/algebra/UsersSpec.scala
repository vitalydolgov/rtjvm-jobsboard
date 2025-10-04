package com.example.jobsboard.algebra

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import doobie.implicits.*

import com.example.jobsboard.domain.user.*
import com.example.jobsboard.fixtures.*
import org.postgresql.util.PSQLException

class UsersSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Inside
    with DoobieSpec
    with UsersFixture {
  val initScript: String = "sql/users.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Users 'algebra'" - {
    "should retrieve a user by email" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userOpt <- users.find("john@acme.com")
        } yield userOpt

        program.asserting(_ shouldBe Some(John))
      }
    }

    "should return None if email doesn't exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userOpt <- users.find("nobody@example.com")
        } yield userOpt

        program.asserting(_ shouldBe None)
      }
    }

    "should create a new user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userId <- users.create(Carol)
          userOpt <- sql"SELECT * FROM users WHERE email = ${Carol.email}"
            .query[User]
            .option
            .transact(xa)
        } yield (userId, userOpt)

        program.asserting { case (userId, userOpt) =>
          userId shouldBe Carol.email
          userOpt shouldBe Some(Carol)
        }
      }
    }

    "should fail to create user with duplicate email" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userId <- users.create(John).attempt
        } yield userId

        program.asserting { outcome =>
          inside(outcome) {
            case Left(error) => error shouldBe a[PSQLException]
            case _           => fail()
          }
        }
      }
    }

    "should update an existing user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userOpt <- users.update(JohnUpdated)
        } yield userOpt

        program.asserting(_ shouldBe Some(JohnUpdated))
      }
    }

    "should return None when updating non-existent user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userOpt <- users.update(Carol)
        } yield userOpt

        program.asserting(_ shouldBe None)
      }
    }

    "should delete a user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          deleted <- users.delete("john@acme.com")
          userOpt <- sql"SELECT * FROM users WHERE email = 'john@acme.com'"
            .query[User]
            .option
            .transact(xa)
        } yield (deleted, userOpt)

        program.asserting { case (deleted, userOpt) =>
          deleted shouldBe true
          userOpt shouldBe None
        }
      }
    }

    "should not delete a non-existent user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          deleted <- users.delete("nobody@example.com")
        } yield deleted

        program.asserting(_ shouldBe false)
      }
    }
  }
}

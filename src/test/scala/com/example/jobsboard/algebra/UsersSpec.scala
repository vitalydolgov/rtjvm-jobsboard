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
    with UserFixture {
  val initScript: String = "sql/users.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Users 'algebra'" - {
    "should retrieve a user by email" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userOpt <- users.find(adminEmail)
        } yield userOpt

        program.asserting(_ shouldBe Some(Admin))
      }
    }

    "should return None if email doesn't exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userOpt <- users.find(InvalidEmail)
        } yield userOpt

        program.asserting(_ shouldBe None)
      }
    }

    "should create a new user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userId <- users.create(Anna)
          userOpt <- sql"SELECT * FROM users WHERE email = ${annaEmail}"
            .query[User]
            .option
            .transact(xa)
        } yield (userId, userOpt)

        program.asserting { case (userId, userOpt) =>
          userId shouldBe annaEmail
          userOpt shouldBe Some(Anna)
        }
      }
    }

    "should fail to create user with duplicate email" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userId <- users.create(Admin).attempt
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
          userOpt <- users.update(Anna)
        } yield userOpt

        program.asserting(_ shouldBe None)
      }
    }

    "should delete a user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          deleted <- users.delete(johnEmail)
          userOpt <- sql"SELECT * FROM users WHERE email = ${johnEmail}"
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
          deleted <- users.delete(InvalidEmail)
        } yield deleted

        program.asserting(_ shouldBe false)
      }
    }
  }
}

package com.example.jobsboard.algebra

import cats.data.*
import cats.effect.*
import cats.implicits.*
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import org.typelevel.log4cats.Logger

import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.user.*

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[User]]
  def signUp(payload: NewUserPayload): F[Option[User]]
  def changePassword(
      email: String,
      payload: NewPasswordPayload
  ): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]
  def sendPasswordRecoveryToken(email: String): F[Unit]
  def recoverPassword(email: String, token: String, newPassword: String): F[Boolean]
}

class LiveAuth[F[_]: Async: Logger] private (users: Users[F], tokens: Tokens[F], emails: Emails[F])
    extends Auth[F] {

  private def updateUser(user: User, newPassword: String) = for {
    hashedNewPassword <- BCrypt.hashpw[F](newPassword)
    updatedUserOpt <- users.update(user.copy(hashedPassword = hashedNewPassword))
  } yield updatedUserOpt

  override def login(email: String, password: String): F[Option[User]] =
    for {
      userOpt <- users.find(email)
      validatedUserOpt <- userOpt.filterA(user =>
        BCrypt.checkpwBool[F](
          password,
          PasswordHash[BCrypt](user.hashedPassword)
        )
      )
    } yield validatedUserOpt

  override def signUp(payload: NewUserPayload): F[Option[User]] =
    users.find(payload.email).flatMap {
      case Some(_) => None.pure[F]
      case None =>
        for {
          hashedPassword <- BCrypt.hashpw[F](payload.password)
          user <- User(
            payload.email,
            hashedPassword,
            payload.firstName,
            payload.lastName,
            payload.company,
            Role.RECRUITER
          ).pure[F]
          _ <- users.create(user)
        } yield Some(user)
    }

  override def changePassword(
      email: String,
      payload: NewPasswordPayload
  ): F[Either[String, Option[User]]] = {
    def checkAndUpdate(user: User, oldPassword: String, newPassword: String) = for {
      passwordCorrect <- BCrypt.checkpwBool[F](
        oldPassword,
        PasswordHash[BCrypt](user.hashedPassword)
      )
      updateResult <-
        if (passwordCorrect)
          updateUser(user, newPassword).map(Right(_))
        else
          Left("Invalid password").pure[F]
    } yield updateResult

    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) =>
        val NewPasswordPayload(oldPassword, newPassword) = payload
        checkAndUpdate(user, oldPassword, newPassword)
    }
  }

  override def delete(email: String): F[Boolean] = users.delete(email)

  override def sendPasswordRecoveryToken(email: String): F[Unit] =
    tokens.getToken(email).flatMap {
      case Some(token) => emails.sendPasswordRecoveryEmail(email, token)
      case None        => ().pure[F]
    }

  override def recoverPassword(email: String, token: String, newPassword: String): F[Boolean] =
    for {
      userOpt <- users.find(email)
      tokenIsValid <- tokens.checkToken(email, token)
      result <- (userOpt, tokenIsValid) match {
        case (Some(user), true) => updateUser(user, newPassword).map(_.nonEmpty)
        case _                  => false.pure[F]
      }
    } yield result
}

object LiveAuth {
  def apply[F[_]: Async: Logger](
      users: Users[F],
      tokens: Tokens[F],
      emails: Emails[F]
  ): F[LiveAuth[F]] =
    new LiveAuth[F](users, tokens, emails).pure[F]
}

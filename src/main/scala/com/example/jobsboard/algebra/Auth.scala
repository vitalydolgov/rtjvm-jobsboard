package com.example.jobsboard.algebra

import cats.effect.*
import cats.implicits.*
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import org.typelevel.log4cats.Logger

import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.domain.user.*

trait Auth[F[_]] {
  def authenticator: Authenticator[F]
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(payload: NewUserPayload): F[Option[User]]
  def changePassword(
      email: String,
      payload: NewPasswordPayload
  ): F[Either[String, Option[User]]]
}

class LiveAuth[F[_]: Async: Logger] private (
    users: Users[F],
    override val authenticator: Authenticator[F]
) extends Auth[F] {
  def login(email: String, password: String): F[Option[JwtToken]] =
    for {
      userOpt <- users.find(email)
      validatedUserOpt <- userOpt.filterA(user =>
        BCrypt.checkpwBool[F](
          password,
          PasswordHash[BCrypt](user.hashedPassword)
        )
      )
      jwtTokenOpt <- validatedUserOpt.traverse(user => authenticator.create(user.email))
    } yield jwtTokenOpt

  def signUp(payload: NewUserPayload): F[Option[User]] =
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

  def changePassword(
      email: String,
      payload: NewPasswordPayload
  ): F[Either[String, Option[User]]] = {
    def updateUser(user: User, newPassword: String) = for {
      hashedNewPassword <- BCrypt.hashpw[F](newPassword)
      updatedUserOpt <- users.update(user.copy(hashedPassword = hashedNewPassword))
    } yield updatedUserOpt

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
}

object LiveAuth {
  def apply[F[_]: Async: Logger](
      users: Users[F],
      authenticator: Authenticator[F]
  ): F[LiveAuth[F]] = new LiveAuth[F](users, authenticator).pure[F]
}

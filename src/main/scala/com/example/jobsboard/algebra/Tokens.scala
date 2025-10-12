package com.example.jobsboard.algebra

import cats.implicits.*
import cats.effect.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import scala.util.Random

import com.example.jobsboard.config.*

trait Tokens[F[_]] {
  def getToken(email: String): F[Option[String]]
  def checkToken(email: String, token: String): F[Boolean]
}

class LiveTokens[F[_]: MonadCancelThrow: Logger] private (users: Users[F])(
    xa: Transactor[F],
    tokenConfig: TokenConfig
) extends Tokens[F] {

  private def findToken(email: String): F[Option[String]] =
    sql"SELECT token from recovery_tokens WHERE email = $email"
      .query[String]
      .option
      .transact(xa)

  private def randomToken(maxLength: Int): F[String] =
    Random.alphanumeric.map(Character.toUpperCase).take(maxLength).mkString.pure[F]

  private def tokenDuration = tokenConfig.tokenDuration

  private def createToken(email: String): F[String] =
    for {
      token <- randomToken(8)
      _ <-
        sql"""
        INSERT INTO recovery_tokens (email, token, expiration) 
        VALUES ($email, $token, ${System.currentTimeMillis() + tokenDuration})
        """.update.run
          .transact(xa)
    } yield token

  private def updateToken(email: String): F[String] =
    for {
      token <- randomToken(8)
      _ <-
        sql"""
        UPDATE recovery_tokens 
        SET token = $token, expiration = ${System.currentTimeMillis() + tokenDuration}
        WHERE email = $email
        """.update.run
          .transact(xa)
    } yield token

  private def getNewToken(email: String): F[String] =
    findToken(email).flatMap {
      case None    => createToken(email)
      case Some(_) => updateToken(email)
    }

  override def getToken(email: String): F[Option[String]] = users.find(email).flatMap {
    case None    => None.pure[F]
    case Some(_) => getNewToken(email).map(Some(_))
  }

  override def checkToken(email: String, token: String): F[Boolean] =
    sql"""
    SELECT token
    FROM recovery_tokens
    WHERE email = $email AND token = $token AND expiration > ${System.currentTimeMillis()}
    """
      .query[String]
      .option
      .transact(xa)
      .map(_.nonEmpty)
}

object LiveTokens {
  def apply[F[_]: MonadCancelThrow: Logger](
      users: Users[F]
  )(xa: Transactor[F], tokenConfig: TokenConfig): F[LiveTokens[F]] =
    new LiveTokens[F](users)(xa, tokenConfig).pure[F]
}

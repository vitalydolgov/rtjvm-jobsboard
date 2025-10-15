package com.example.jobsboard.modules

import cats.*
import cats.data.*
import cats.implicits.*
import cats.effect.*
import org.http4s.server.Router
import tsec.authentication.{IdentityStore, BackingStore}
import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.authentication.SecuredRequestHandler
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256
import org.typelevel.log4cats.Logger

import com.example.jobsboard.http.routes.*
import com.example.jobsboard.config.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.domain.user.*
import com.example.jobsboard.algebra.*

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]) {
  given SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F](core.jobs).routes
  private val authRoutes = AuthRoutes[F](core.auth, authenticator).routes

  val endpoints = Router {
    "/api" -> (healthRoutes <+> jobRoutes <+> authRoutes)
  }
}

object HttpApi {
  def createAuthenticator[F[_]: Sync](
      users: Users[F],
      securityConfig: SecurityConfig
  ): F[Authenticator[F]] = {
    val identityStore: IdentityStore[F, String, User] = (email: String) =>
      OptionT(users.find(email))

    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          OptionT(ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[AugmentedJWT[HMACSHA256, String]] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def update(v: JwtToken): F[AugmentedJWT[HMACSHA256, String]] =
          put(v)

        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
      }
    }

    val signingKeyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    for {
      signingKey <- signingKeyF
      tokenStore <- tokenStoreF
    } yield JWTAuthenticator.backed.inBearerToken(
      expiryDuration = securityConfig.jwtExpiryDuration,
      maxIdle = None,
      tokenStore,
      identityStore,
      signingKey
    )
  }

  def apply[F[_]: Async: Logger](
      core: Core[F],
      securityConfig: SecurityConfig
  ): Resource[F, HttpApi[F]] =
    Resource
      .eval(createAuthenticator(core.users, securityConfig))
      .map(authenticator => new HttpApi[F](core, authenticator))

}

package com.example.jobsboard.domain

import cats.*
import cats.implicits.*
import tsec.authentication.{AugmentedJWT, JWTAuthenticator, SecuredRequest, TSecAuthService}
import tsec.authorization.{BasicRBAC, AuthorizationInfo}
import tsec.mac.jca.HMACSHA256
import org.http4s.{Response, Status}

import com.example.jobsboard.domain.user.*

object security {
  type JwtToken = AugmentedJWT[HMACSHA256, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, HMACSHA256]
  type AuthRoute[F[_]] = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  type AuthRbac[F[_]] = BasicRBAC[F, Role, User, JwtToken]

  given authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(user: User): F[Role] = user.role.pure[F]
  }

  def allRoles[F[_]: MonadThrow]: AuthRbac[F] =
    BasicRBAC.all[F, Role, User, JwtToken]

  def adminOnly[F[_]: MonadThrow]: AuthRbac[F] =
    BasicRBAC(Role.ADMIN)

  def recruiterOnly[F[_]: MonadThrow]: AuthRbac[F] =
    BasicRBAC(Role.RECRUITER)

  case class Authorizations[F[_]](rbacRoutes: Map[AuthRbac[F], List[AuthRoute[F]]])

  object Authorizations {
    given combiner[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance { (authA, authB) =>
      Authorizations(authA.rbacRoutes |+| authB.rbacRoutes)
    }
  }

  extension [F[_]](authRoute: AuthRoute[F])
    def restrictedTo(rbac: AuthRbac[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))

  given authToTSec[F[_]: Monad]: Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] =
    auths => {
      val unauthorizedService: TSecAuthService[User, JwtToken, F] =
        TSecAuthService[User, JwtToken, F] { _ =>
          Response[F](Status.Unauthorized).pure[F]
        }

      auths.rbacRoutes.toSeq
        .foldLeft(unauthorizedService) { case (acc, (rbac, routes)) =>
          val route = routes.reduce(_.orElse(_))
          TSecAuthService.withAuthorizationHandler(rbac)(route, acc.run)
        }
    }
}

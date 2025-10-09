package com.example.jobsboard.domain

import tsec.authentication.{AugmentedJWT, JWTAuthenticator, SecuredRequest}
import org.http4s.Response
import tsec.mac.jca.HMACSHA256

import com.example.jobsboard.domain.user.*

object security {
  type JwtToken = AugmentedJWT[HMACSHA256, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, HMACSHA256]
  type AuthRoute[F[_]] = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
}

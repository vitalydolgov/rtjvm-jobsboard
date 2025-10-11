package com.example.jobsboard.fixtures

import cats.data.*
import cats.effect.*
import org.http4s.*
import org.http4s.headers.Authorization
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.jws.mac.JWTMac
import scala.concurrent.duration.*

import com.example.jobsboard.domain.security.*
import com.example.jobsboard.domain.user.*

trait SecuredRouteFixture extends UserFixture {
  val mockedAuthenticator: Authenticator[IO] = {
    val key = HMACSHA256.unsafeGenerateKey

    val identityStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == adminEmail) OptionT.pure(Admin)
      else if (email == johnEmail) OptionT.pure(John)
      else OptionT.none[IO, User]

    JWTAuthenticator.unbacked.inBearerToken(
      1.day,
      None,
      identityStore,
      key
    )
  }

  extension (r: Request[IO])
    def withBearerToken(jwtToken: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](jwtToken.jwt)
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }
}

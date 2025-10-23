package com.example.jobsboard

import cats.effect.*
import cats.implicits.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.middleware.CORS
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import doobie.*
import doobie.implicits.*

import com.example.jobsboard.modules.*
import com.example.jobsboard.config.*
import com.example.jobsboard.config.syntax.*

object Application extends IOApp.Simple {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private def databaseHealth[F[_]: Async: Logger](xa: Transactor[F]): F[Boolean] = {
    sql"SELECT 1".query[Int].unique.transact(xa).attempt.map(_.isRight)
  }

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig, securityConfig, tokenConfig, emailServiceConfig) =>
      val appResource = for {
        xa <- Database.makePostgresResource[IO](postgresConfig)
        dbReady <- Resource.eval[IO, Boolean](databaseHealth(xa))
        core <- Core[IO](xa, tokenConfig, emailServiceConfig)
        httpApi <- HttpApi[IO](core, securityConfig)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(CORS(httpApi.endpoints).orNotFound)
          .build
      } yield (server, dbReady)

      appResource.use { (_, dbReady) =>
        val warnIssues =
          if (!dbReady) Logger[IO].warn(s"Database is not reachable!") else IO.unit
        warnIssues *> IO.println("Server is ready!") *> IO.never
      }
  }
}

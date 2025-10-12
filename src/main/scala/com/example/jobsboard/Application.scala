package com.example.jobsboard

import cats.effect.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import com.example.jobsboard.modules.*
import com.example.jobsboard.config.*
import com.example.jobsboard.config.syntax.*

object Application extends IOApp.Simple {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig, securityConfig) =>
      val appResource = for {
        xa <- Database.makePostgresResource[IO](postgresConfig)
        core <- Core[IO](xa)
        httpApi <- HttpApi[IO](core, securityConfig)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(httpApi.endpoints.orNotFound)
          .build
      } yield server

      appResource.use(_ => IO.println("Server is ready!") *> IO.never)
  }
}

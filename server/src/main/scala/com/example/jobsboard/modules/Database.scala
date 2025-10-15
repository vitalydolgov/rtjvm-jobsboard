package com.example.jobsboard.modules

import cats.effect.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

import com.example.jobsboard.config.PostgresConfig

object Database {
  def makePostgresResource[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        config.url,
        config.username,
        config.password,
        ec
      )
    } yield xa
}

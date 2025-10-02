package com.example.jobsboard.modules

import cats.*
import cats.implicits.*
import cats.effect.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

import com.example.jobsboard.algebra.*

final class Core[F[_]] private (val jobs: Jobs[F])

object Core {
  def apply[F[_]: Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource
      .eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
}

package com.example.jobsboard.playground

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor
import com.example.jobsboard.domain.Job.*
import com.example.jobsboard.modules.LiveJobs
import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {
  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    company = "ACME",
    title = "Scala Developer",
    description = "Cats, Cats Effect, ScalaJS",
    externalUrl = "example.com",
    remote = false,
    location = "NYC"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs <- LiveJobs[IO](xa)
      _ <- IO(println("Ready. Next...")) *> IO(StdIn.readLine)
      id <- jobs.create("vitaly@example.com", jobInfo)
      _ <- IO(println("Next...")) *> IO(StdIn.readLine)
      list <- jobs.all()
      _ <- IO(println(s"All jobs: $list. Next...")) *> IO(StdIn.readLine)
      _ <- jobs.update(id, jobInfo.copy(title = "Scala Rockstar"))
      newJob <- jobs.find(id)
      _ <- IO(println(s"New job: $newJob. Next...")) *> IO(StdIn.readLine)
      _ <- jobs.delete(id)
      listAfter <- jobs.all()
      _ <- IO(println(s"Deleted job. All jobs: $listAfter")) *> IO(StdIn.readLine)
    } yield ()
  }
}

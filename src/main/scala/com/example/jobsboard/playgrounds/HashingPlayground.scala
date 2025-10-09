package com.example.jobsboard.playgrounds

import cats.effect.*
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash

object HashingPlayground extends IOApp.Simple {

  override def run: IO[Unit] =
    BCrypt
      .hashpw[IO]("passw0rd")
      .flatMap(IO.println)

}

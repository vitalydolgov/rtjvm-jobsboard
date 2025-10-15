package com.example.jobsboard.domain

object pagination {
  final case class Pagination(limit: Int, offset: Int)

  object Pagination {
    val defaultPageSize = 20

    def apply(limitOpt: Option[Int], offsetOpt: Option[Int]) =
      new Pagination(limitOpt.getOrElse(defaultPageSize), offsetOpt.getOrElse(0))

    def default = new Pagination(defaultPageSize, 0)
  }
}

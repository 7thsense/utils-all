package com.theseventhsense.utils.persistence.db

import akka.stream.scaladsl.Source
import com.theseventhsense.utils.persistence.{
  AbstractQueryMeta,
  StreamQueryResult
}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait SlickStreamingHelpers[P <: JdbcProfile, T] extends SlickImplicits[P] {
  import profile.api._

  def createStream[A, B, C](
    query: Query[A, B, Seq],
    meta: AbstractQueryMeta
  ): Source[B, akka.NotUsed] = {
    Source.fromPublisher(
      db.stream(query.withCriteria(meta).withRange(meta).result)
    )
  }

  def createStreamResult[A, B, C](
    query: Query[A, B, Seq],
    meta: AbstractQueryMeta,
    countQuery: Option[Query[A, C, Seq]] = None
  )(implicit ec: ExecutionContext): Future[StreamQueryResult[B]] = {
    val totalFut =
      db.run(countQuery.getOrElse(query).withCriteria(meta).length.result)
    val recordsQuery = if (countQuery.isDefined) {
      query.withCriteria(meta)
    } else {
      query.withCriteria(meta).withRange(meta)
    }
    val records = db.stream(recordsQuery.result)
    totalFut.map { total =>
      StreamQueryResult(total.toLong, Source.fromPublisher(records))
    }
  }
}

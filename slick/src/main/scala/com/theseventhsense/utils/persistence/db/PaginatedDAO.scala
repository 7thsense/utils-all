package com.theseventhsense.utils.persistence.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.theseventhsense.utils.persistence.AbstractQueryMeta
import com.theseventhsense.utils.persistence.ListQueryResult
import slick.jdbc.JdbcProfile

/**
  * Created by erik on 2/22/16.
  */
trait PaginatedDAO[P <: JdbcProfile, Id <: BaseId, T <: Identified[Id, _]] {
  self: SlickBaseDAO[P, Id, T] =>

  import profile.api._

  def find(meta: AbstractQueryMeta)(implicit ec: ExecutionContext): Future[ListQueryResult[T]] = {
    createFindResult(table.sortBy(_.id), meta)
  }

  def createFindResult[A, B](
    query: Query[A, B, Seq],
    meta: AbstractQueryMeta,
    countQuery: Option[Query[A, _, Seq]] = None
  )(implicit ec: ExecutionContext): Future[ListQueryResult[B]] = {
    val totalFut =
      db.run(countQuery.getOrElse(query).withCriteria(meta).length.result)
    val recordsQuery = if (countQuery.isDefined) {
      query.withCriteria(meta)
    } else {
      query.withCriteria(meta).withRange(meta)
    }
    val recordsFut = db.run(recordsQuery.result)
    val listQuery = for {
      total: Int <- totalFut
      records <- recordsFut
    } yield ListQueryResult(total.toLong, records.toList)
    listQuery
  }

}

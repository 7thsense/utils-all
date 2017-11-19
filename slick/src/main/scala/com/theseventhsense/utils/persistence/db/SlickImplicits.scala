package com.theseventhsense.utils.persistence.db

import com.theseventhsense.utils.persistence.AbstractQueryMeta
import com.theseventhsense.utils.types.SSDateTime
import slick.jdbc.JdbcProfile

/**
  * Created by erik on 2/22/16.
  */
trait SlickImplicits[P <: JdbcProfile] extends HasDatabaseConfig[P] {
  import profile.api._

  implicit class RichMetaQuery[A, B](q: Query[A, B, Seq]) {
    def withRange(implicit meta: AbstractQueryMeta): Query[A, B, Seq] = {
      var records = q.drop(meta.offset)
      meta.limit.foreach { limit =>
        records = records.take(limit)
      }
      records
    }

    def withCriteria(implicit meta: AbstractQueryMeta): Query[A, B, Seq] = {
      applyMetaQ(sortByMeta(q, meta), meta)
    }
  }

  /**
    * Apply sort criteria from meta.
    *
    * @param query
    * @param meta
    * @tparam A
    * @tparam B
    * @return
    */
  protected def sortByMeta[A, B](query: Query[A, B, Seq],
                                 meta: AbstractQueryMeta): Query[A, B, Seq] = {
    query
  }

  //  protected def sortByMeta[A <: IdentifiedTable[_], B](query: Query[A, B, Seq], meta: QueryMeta) = {
  //    var q = query
  //    meta.sort match {
  //      case None =>
  //        if (meta.sortAsc) {
  //          q = q.sortBy(_.id)
  //        } else {
  //          q = q.sortBy(_.id.desc)
  //        }
  //      case Some(s: String) =>
  //        if (meta.sortAsc) {
  //          q = q.sortBy(_.id)
  //        } else {
  //          q = q.sortBy(_.id.desc)
  //        }
  //    }
  //    q
  //  }

  protected def applyMetaQ[A, B](query: Query[A, B, Seq],
                                 meta: AbstractQueryMeta): Query[A, B, Seq] = {
    query
  }

  implicit val SSInstantMapper: driver.BaseColumnType[
    com.theseventhsense.utils.types.SSDateTime.Instant
  ] =
    MappedColumnType.base[SSDateTime.Instant, java.sql.Timestamp](
      d => new java.sql.Timestamp(d.millis),
      d => new SSDateTime.Instant(d.getTime)
    )

  implicit val SSTimeZoneMapper: driver.BaseColumnType[
    com.theseventhsense.utils.types.SSDateTime.TimeZone
  ] =
    MappedColumnType.base[SSDateTime.TimeZone, String](
      t => t.name,
      t => SSDateTime.TimeZone.from(t)
    )

}

package com.theseventhsense.utils.persistence

import scala.util.Try

case class QueryMeta(override val rangeStart: Long = 1,
                     override val rangeEnd: Option[Long] = None,
                     override val sort: Option[String] = None,
                     override val sortAsc: Boolean = true,
                     override val totalCount: Option[Long] = None,
                     override val filename: Option[String] = None,
                     override val filter: Option[String] = None,
                     override val q: Option[String] = None)
    extends AbstractQueryMeta {

  def withoutRange: QueryMeta = this.copy(rangeStart = 1, rangeEnd = None)

  def withLimit(limit: Long): QueryMeta =
    copy(rangeEnd = Some(rangeStart + limit))

  def withOffset(offset: Long): QueryMeta =
    copy(rangeStart = offset + 1, rangeEnd.map(_ - rangeStart + offset + 1))

  def toHeaders: Seq[(String, String)] = {
    var response = totalCount match {
      case None =>
        Seq("Content-Range" -> s"entities $rangeStart-${rangeEnd.getOrElse(0)}")
      case Some(x: Long) =>
        Seq(
          "X-Total-Count" -> x.toString,
          "Content-Range" -> s"entities $rangeStart-${rangeEnd.getOrElse(0)}/$x"
        )
    }
    filename.foreach { f =>
      val value = s"""attachment; filename="$f""""
      response = response ++ Seq("Content-Disposition" -> value)
    }
    // Add access-control-expose-headers since just in case we need to access
    // the resource via CORS
    // See http://stackoverflow.com/questions/25673089/why-is-access-control-expose-headers-needed
    response ++ Seq("Access-Control-Expose-Headers" -> "true")
  }

  /**
    * Update the ListMeta from a ListQuery
    *
    * @param listQuery
    * @return
    */
  def copyFrom[T <: DomainObject](listQuery: ListQueryResult[T]): QueryMeta = {
    this.copy(
      totalCount = Some(listQuery.totalCount),
      rangeEnd = if (listQuery.data.isEmpty) {
        Some(this.rangeStart)
      } else {
        Some(rangeStart + listQuery.data.length - 1)
      }
    )
  }

  /**
    * Update the ListMeta from a ListQuery
    *
    * @param listQuery
    * @return
    */
  def copyFrom[T <: DomainObject](listQuery: QueryResult): QueryMeta = {
    this.copy(
      totalCount = Some(listQuery.totalCount),
      rangeEnd = rangeEnd.orElse(Some(rangeStart + listQuery.totalCount - 1))
    )
  }

  def copyFromHeaders(headers: Map[String, String]): QueryMeta = {
    headers.get("Range") match {
      case Some(x: String) =>
        try {
          val rangeStartStr :: rangeEndStr :: extra = x.split("\\D").toList
          val rangeEnd = Try(rangeEndStr.toLong).toOption
          this.copy(rangeStart = rangeStartStr.toLong, rangeEnd = rangeEnd)
        } catch {
          case e: MatchError =>
            this
        }
      case _ =>
        this
    }
  }

}

object QueryMeta {

  def fromOffset(offset: Long = 0,
                 limitOpt: Option[Long] = None,
                 filename: Option[String] = None): QueryMeta = {
    val rangeStart = offset + 1
    val rangeEnd = limitOpt.map { limit: Long =>
      limit + rangeStart - 1
    }
    QueryMeta(rangeStart, rangeEnd, filename = filename)
  }

  def fromParams(params: Map[String, Seq[String]],
                 sortAscDefault: Boolean = false): QueryMeta = {
    val sort: Option[String] =
      params.get("sortField").orElse(params.get("sort")).flatMap(_.headOption)

    val sortAsc: Boolean = params.get("sortAsc") match {
      case Some(dir: Seq[String]) => dir.headOption.contains("true")
      case None =>
        params.get("sortDir") match {
          case None                 => sortAscDefault
          case Some(x: Seq[String]) => !x.headOption.contains("DESC")
        }
    }
    val filename: Option[String] = params.get("filename").flatMap(_.headOption)
    val qWithRange =
      if (params.contains("rangeStart") && params.contains("rangeEnd")) {
        val rangeStart: Long =
          params.getOrElse("rangeStart", Seq("1")).head.toLong
        val rangeEnd: Long =
          params.getOrElse("rangeEnd", Seq("100")).head.toLong
        QueryMeta(
          rangeStart,
          Some(rangeEnd)
        )
      } else if (params.contains("offset") || params.contains("limit")) {
        val offset: Long =
          params.get("offset").map(_.head.toLong).getOrElse(0L)
        val limit = params.get("limit").map(_.head.toLong)
        QueryMeta.fromOffset(offset, limit)
      } else {
        val page = params.getOrElse("page", Seq("1")).head.toLong
        val pageSize = params.getOrElse("pageSize", Seq("100")).head.toLong
        val rangeStart: Long = ((page - 1) * pageSize) + 1
        val rangeEnd: Long = rangeStart + pageSize - 1
        val q: QueryMeta =
          QueryMeta(rangeStart, Some(rangeEnd))
        q
      }
    qWithRange.copy(
      sort = sort,
      sortAsc = sortAsc,
      filename = filename,
      q = params.get("q").flatMap(_.headOption),
      filter = params.get("filter").flatMap(_.headOption)
    )
  }
}

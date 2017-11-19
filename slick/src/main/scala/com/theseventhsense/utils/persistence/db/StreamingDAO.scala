package com.theseventhsense.utils.persistence.db

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.theseventhsense.utils.persistence.AbstractQueryMeta
import com.theseventhsense.utils.persistence.StreamQueryResult
import slick.jdbc.JdbcProfile

trait StreamingDAO[P <: JdbcProfile, Id <: BaseId, T <: Identified[Id, _]]
    extends SlickStreamingHelpers[P, T] { self: SlickBaseDAO[P, Id, T] =>

  import profile.api._

  def stream(meta: AbstractQueryMeta): Future[StreamQueryResult[T]] = {
    createStreamResult(table.sortBy(_.id), meta)
  }
}

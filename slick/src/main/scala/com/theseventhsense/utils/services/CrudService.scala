package com.theseventhsense.utils.services

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.theseventhsense.utils.persistence.QueryMeta
import com.theseventhsense.utils.persistence.StreamQueryResult
import com.theseventhsense.utils.persistence.db.BaseId
import com.theseventhsense.utils.persistence.db.Identified

/**
  * Created by erik on 12/22/16.
  */
trait CrudService[Id <: BaseId, T <: Identified[Id, T]] {
  def get(id: Id)(implicit ec: ExecutionContext): Future[Option[T]]
  def create(t: T)(implicit ec: ExecutionContext): Future[T]
  def save(t: T)(implicit ec: ExecutionContext): Future[T]
  def delete(id: Id)(implicit ec: ExecutionContext): Future[Int]
  def stream(meta: QueryMeta)(
    implicit ec: ExecutionContext
  ): Future[StreamQueryResult[T]]
}

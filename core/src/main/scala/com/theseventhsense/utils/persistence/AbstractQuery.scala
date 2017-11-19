package com.theseventhsense.utils.persistence

/**
  * Created by erik on 2/4/16.
  */
trait AbstractQueryMeta {
  def rangeStart: Long = 1
  def rangeEnd: Option[Long] = None
  def sort: Option[String] = None
  def sortAsc: Boolean = true
  def totalCount: Option[Long] = None
  def filename: Option[String] = None
  def filter: Option[String] = None
  def q: Option[String] = None

  def offset: Long = {
    rangeStart - 1
  }

  def limit: Option[Long] = {
    rangeEnd.map { end =>
      end - rangeStart + 1
    }
  }
}

trait AbstractQuery {
  def meta: AbstractQueryMeta
}

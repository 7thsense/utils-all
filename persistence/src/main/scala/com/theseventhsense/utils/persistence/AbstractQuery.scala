package com.theseventhsense.utils.persistence

import com.netaporter.uri.QueryString

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
  def format: Option[String] = None

  def offset: Long = {
    rangeStart - 1
  }

  def limit: Option[Long] = {
    rangeEnd.map { end =>
      end - rangeStart + 1
    }
  }

  def queryString: QueryString =
    QueryString(
      Seq(
        "rangeStart" -> Some(rangeStart.toString),
        "sortAsc" -> Some(sortAsc.toString)
      ) ++
        rangeEnd.map(x => "rangeEnd" -> Some(x.toString)).toSeq ++
        sort.map(x => "sort" -> Some(x.toString)).toSeq ++
        filename.map(x => "filename" -> Some(x.toString)).toSeq ++
        filter.map(x => "filter" -> Some(x.toString)).toSeq ++
        q.map(x => "q" -> Some(x.toString)).toSeq ++
        format.map(x => "format" -> Some(x.toString)).toSeq
    )
}

trait AbstractQuery {
  def meta: AbstractQueryMeta
}

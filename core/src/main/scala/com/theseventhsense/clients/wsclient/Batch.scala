package com.theseventhsense.clients.wsclient

import com.theseventhsense.utils.persistence.Keyed

trait Batch[T <: Keyed] {
  def nextOffset: Option[String]

  def items: Seq[T]

  def hasMore: Boolean = nextOffset.isDefined
}

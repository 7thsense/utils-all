package com.theseventhsense.clients.wsclient

import com.theseventhsense.utils.persistence.Keyed

import scala.concurrent.Await
import scala.concurrent.duration._

class BatchIterator[T <: Keyed](loader: BatchLoader[T]) extends Iterator[T] {
  val timeout = 15.minutes
  var last: Option[Batch[T]] = None
  var iter: Iterator[T] = Iterator.empty

  override def hasNext: Boolean = {
    if (last.isEmpty) {
      load()
    } else if (!iter.hasNext && last.get.nextOffset.isDefined) {
      load()
    }
    iter.hasNext
  }

  def load(): Unit = {
    last = last match {
      case None =>
        Some(Await.result(loader.load(None), timeout))
      case Some(x) =>
        x.nextOffset match {
          case Some(x) =>
            Some(Await.result(loader.load(Some(x)), timeout))
          case None =>
            throw new RuntimeException("Failing loading empty BatchLoader")
        }
    }
    iter = last.get.items.iterator
  }

  override def next(): T = {
    iter.next()
  }
}

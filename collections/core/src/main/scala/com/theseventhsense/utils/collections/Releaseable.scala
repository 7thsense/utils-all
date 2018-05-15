package com.theseventhsense.utils.collections

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * Created by erik on 3/31/16.
  */
trait Releasable {
  def release: Future[Unit]
}

trait ReleasableItem[T] extends Releasable {
  def item: T
}

trait ReleasableIterable[A, C <: Iterable[A]]
    extends ReleasableItem[C]
    with Iterable[A] {
  import ReleasableIterable._
  override def iterator: Iterator[A] = item.iterator
  def releasableIterator: SizedIterator[A] =
    SizedIterator(this).withOnEmpty(() => Await.result(this.release, TimeOut))
}

object ReleasableIterable {
  def TimeOut: FiniteDuration = 1.second
}

case class NoOpReleasableIterable[A, C <: Iterable[A]](item: C)
    extends ReleasableIterable[A, C] {
  def release: Future[Unit] = Future.successful(())
}

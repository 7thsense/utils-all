package com.theseventhsense.clients.wsclient

import akka.NotUsed
import akka.stream.scaladsl.{RestartSource, Source}
import com.theseventhsense.utils.persistence.Keyed

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait BatchLoader[T <: Keyed] {
  def load(offset: Option[String]): Future[Batch[T]]

  private def extract(b: Batch[T]): (Option[String], List[T]) =
    (b.nextOffset, b.items.toList)

  private def loadAndExtract(
    offset: Option[String]
  )(implicit ec: ExecutionContext): Future[Option[(Option[String], List[T])]] =
    load(offset).map(b => Option(extract(b)))

  def source(implicit ec: ExecutionContext): Source[T, NotUsed] = {
    RestartSource.withBackoff(
      minBackoff = 1.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ) { () =>
      Source
        .unfoldAsync(Option.empty[String])(loadAndExtract)
        .mapConcat(identity)
    }
  }

  def iterator: Iterator[T] = new BatchIterator[T](this)
}

trait ParentBatchLoader[A <: KeyedTimestamp, B <: KeyedTimestamp]
    extends BatchLoader[A] {
  val MaxGroupSize = 100000
  val MaxOpenSubStreams = 10
  val state: StateHandler[A] = new LastModifiedStateHandler[A]()

  def childLoaders(a: A): immutable.Seq[BatchLoader[B]]

  def childSource(implicit ec: ExecutionContext): Source[B, NotUsed] = {
    source
      .filter(state.filter)
      .grouped(MaxGroupSize)
      .map { group =>
        group.sorted(state.ordering).reverse
      }
      .mapConcat(identity)
      .mapConcat { a: A =>
        childLoaders(a).map(_.source.map(event => (a, event)))
      }
      .flatMapMerge(MaxOpenSubStreams, identity)
      .map {
        case (a, b) =>
          state.update(a.key, b.timestamp)
          b
      }
  }

  def childIterator: Iterator[B] =
    this.iterator
      .filter(state.filter)
      .toList
      .sorted(state.ordering)
      .reverse
      .foldLeft[Iterator[B]](Iterator.empty) {
        case (aIter, a) =>
          aIter ++ childLoaders(a).foldLeft[Iterator[B]](Iterator.empty) {
            case (bIter, child) =>
              bIter ++ child.iterator.map { b =>
                state.update(a.key, b.timestamp)
                b
              }
          }
      }
}

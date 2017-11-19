package com.theseventhsense.clients.wsclient

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import com.theseventhsense.utils.persistence.Keyed

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait BatchLoader[T <: Keyed] {
  def load(offset: Option[String]): Future[Batch[T]]

  def source(implicit ec: ExecutionContext): Source[T, ActorRef] = {
    Source.actorPublisher(BatchSource.props[T](this))
  }

  def iterator: Iterator[T] = new BatchIterator[T](this)
}

trait ParentBatchLoader[A <: KeyedTimestamp, B <: KeyedTimestamp]
    extends BatchLoader[A] {
  val MaxGroupSize = 100000
  val MaxOpenSubStreams = 10
  val state: StateHandler[A] = new LastModifiedStateHandler[A]()

  def childLoaders(a: A): immutable.Seq[BatchLoader[B]]

  def childSource(implicit ec: ExecutionContext): Source[B, ActorRef] = {
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

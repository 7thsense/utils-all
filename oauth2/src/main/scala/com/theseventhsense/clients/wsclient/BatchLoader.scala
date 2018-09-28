package com.theseventhsense.clients.wsclient

import akka.NotUsed
import akka.stream.scaladsl.Source

import com.theseventhsense.utils.logging.{LogContext, Logging}
import com.theseventhsense.utils.persistence.Keyed
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait BatchLoader[T <: Keyed] extends Logging {
  def load(offset: Option[String]): Future[Batch[T]]

  private case class UnfoldBatch(offset: Option[String] = None,
                                 isComplete: Boolean = false,
                                 items: List[T] = List.empty)

  private def loadAndExtract(
    lastBatch: UnfoldBatch
  )(implicit ec: ExecutionContext, lc: LogContext): Future[Option[(UnfoldBatch, List[T])]] =
    if (lastBatch.isComplete && lastBatch.items.isEmpty) {
      logger.trace(s"Finished loading $lastBatch")
      Future.successful(None)
    } else if (lastBatch.isComplete && lastBatch.items.nonEmpty) {
      logger.trace(s"Flushing queue $lastBatch")
      Future.successful(
        Some((lastBatch.copy(items = List.empty), lastBatch.items))
      )
    } else {
      logger.trace(s"Loading data $lastBatch")
      load(lastBatch.offset).map(
        result =>
          Some(
            (
              UnfoldBatch(
                offset = result.nextOffset,
                isComplete = !result.hasMore,
                items = result.items.toList
              ),
              lastBatch.items
            )
        )
      )
    }

  def source(implicit ec: ExecutionContext, lc: LogContext): Source[T, NotUsed] =
    Source
      .unfoldAsync(UnfoldBatch())(loadAndExtract)
      .mapConcat(identity)

  def iterator: Iterator[T] = new BatchIterator[T](this)
}

trait ParentBatchLoader[A <: KeyedTimestamp, B <: KeyedTimestamp]
    extends BatchLoader[A] {
  val MaxGroupSize = 100000
  val MaxOpenSubStreams = 10
  val state: StateHandler[A] = new LastModifiedStateHandler[A]()

  def childLoaders(a: A): immutable.Seq[BatchLoader[B]]

  def childSource(implicit ec: ExecutionContext, lc: LogContext): Source[B, NotUsed] = {
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

  def childIterator(implicit lc: LogContext): Iterator[B] =
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

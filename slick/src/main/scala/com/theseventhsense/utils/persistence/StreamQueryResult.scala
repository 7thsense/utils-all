package com.theseventhsense.utils.persistence

import scala.language.implicitConversions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

object StreamQueryResult {
  implicit def convert[A, B](
    a: StreamQueryResult[A]
  )(implicit converter: (A) => B): StreamQueryResult[B] = {
    val stream: Source[B, _] = a.stream.map { item =>
      converter(item)
    }
    StreamQueryResult[B](a.totalCount, stream)
  }

  implicit def convertFuture[A, B](a: Future[StreamQueryResult[A]])(
    implicit converter: (A) => B,
    ec: ExecutionContext
  ): Future[StreamQueryResult[B]] = {
    a.map { source: StreamQueryResult[A] =>
      val result: StreamQueryResult[B] = convert(source)
      result
    }
  }

  def fromSeq[T](s: Seq[T]): StreamQueryResult[T] =
    StreamQueryResult[T](s.size.toLong, Source.fromIterator(() => s.iterator))
}

case class StreamQueryResult[T](totalCount: Long, stream: Source[T, _])
    extends QueryResult {
  def map[U](converter: (T) => U): StreamQueryResult[U] = {
    val s = stream.map(converter)
    StreamQueryResult(totalCount, s)
  }

  def collect[U](pf: PartialFunction[T, U]): StreamQueryResult[U] = {
    val s = stream.collect(pf)
    StreamQueryResult(totalCount, s)
  }

  def flatten(implicit materializer: Materializer): Future[Seq[T]] = {
    if (totalCount > 0) {
      stream.grouped(totalCount.toInt).runWith(Sink.head)
    } else {
      Future.successful(Seq.empty)
    }
  }
}

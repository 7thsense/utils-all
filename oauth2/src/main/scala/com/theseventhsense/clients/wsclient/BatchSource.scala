package com.theseventhsense.clients.wsclient

import akka.actor.{Props, Status}
import akka.pattern.pipe
import akka.stream.actor.ActorPublisher
import com.theseventhsense.utils.persistence.Keyed

import scala.concurrent.ExecutionContext

class BatchSource[A <: Keyed](
  loader: BatchLoader[A]
)(implicit ec: ExecutionContext)
    extends ActorPublisher[A] {

  import akka.stream.actor.ActorPublisherMessage._

  final val BUFFER_AMOUNT = 1000
  private var first = true
  private var nextOffset: Option[String] = None
  private var buffer: Seq[A] = Seq.empty

  def receive: Receive = waitingForDownstreamReq(0)

  case object Pull

  private def shouldLoadMore = {
    nextOffset.isDefined && (totalDemand > 0 || buffer.length < BUFFER_AMOUNT)
  }

  def waitingForDownstreamReq(offset: Long): Receive = {
    case Request(_) | Pull =>
      val sent = if (buffer.nonEmpty) {
        sendFromBuff(totalDemand)
      } else {
        0
      }
      if (first || (shouldLoadMore && isActive)) {
        first = false
        loader.load(nextOffset).pipeTo(self)
        context.become(waitingForFut(offset + sent, totalDemand))
      }

    case Cancel => context.stop(self)
  }

  def sendFromBuff(demand: Long): Long = {
    val consumed = buffer.take(demand.toInt).toList
    buffer = buffer.drop(consumed.length)
    consumed.foreach(onNext)
    if (nextOffset.isEmpty && buffer.isEmpty) {
      onComplete()
    }
    consumed.length.toLong
  }

  def waitingForFut(s: Long, beforeFutDemand: Long): Receive = {
    case batch: Batch[A] =>
      nextOffset = if (batch.items.isEmpty) {
        None
      } else {
        batch.nextOffset
      }
      buffer = buffer ++ batch.items
      val consumed = sendFromBuff(beforeFutDemand)
      self ! Pull
      context.become(waitingForDownstreamReq(s + consumed))

    case Request(_) | Pull => // ignoring until we receive the future response

    case Status.Failure(err) =>
      context.become(waitingForDownstreamReq(s))
      onError(err)

    case Cancel => context.stop(self)
  }
}

object BatchSource {
  def props[T <: Keyed](
    loader: BatchLoader[T]
  )(implicit ec: ExecutionContext): Props = {
    Props(new BatchSource[T](loader))
  }
}

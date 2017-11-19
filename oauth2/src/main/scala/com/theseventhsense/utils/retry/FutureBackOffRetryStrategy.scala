package com.theseventhsense.utils.retry

import akka.actor.ActorSystem
import akka.pattern.after
import com.theseventhsense.utils.logging.Logging

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class FutureBackOffRetryStrategyWithCriteria(
  firstDelay: FiniteDuration,
  maxCount: Int = 10,
  maxDelay: FiniteDuration = 1.hour,
  factor: Double = 2D
)(implicit system: ActorSystem, ec: ExecutionContext)
    extends FutureRetryStrategyWithCriteria
    with Logging {

  override def retry[T](producer: => Future[T],
                        passCriteria: (T) => Boolean,
                        failCriteria: (Throwable) => Boolean): Future[T] =
    recursiveRetry(
      producer,
      passCriteria = passCriteria,
      failCriteria = failCriteria
    )

  protected def recursiveRetry[T](
    producer: => Future[T],
    delay: FiniteDuration = firstDelay,
    count: Long = 1L,
    passCriteria: (T) => Boolean,
    failCriteria: (Throwable) => Boolean
  ): Future[T] = {
    def delayAndRetry: Future[T] = {
      logger.trace(s"Delaying by $delay, try $count")
      after(delay, system.scheduler)(Future.successful(true)).flatMap { x =>
        val multiple = math.pow(factor, count.toDouble).toLong
        val computedDelay = firstDelay * multiple
        val nextDelay =
          if (computedDelay > maxDelay) maxDelay else computedDelay
        recursiveRetry(
          producer,
          nextDelay,
          count + 1L,
          passCriteria,
          failCriteria
        )
      }
    }
    producer
      .recoverWith {
        case t =>
          if (count <= maxCount && !failCriteria(t)) {
            delayAndRetry
          } else {
            Future.failed(t)
          }
      }
      .flatMap(
        t => if (passCriteria(t)) Future.successful(t) else delayAndRetry
      )
  }
}

class FutureBackOffRetryStrategy(
  firstDelay: FiniteDuration,
  maxCount: Int = 10,
  maxDelay: FiniteDuration = 1.hour,
  factor: Double = 2D,
  shouldRetry: Throwable => Boolean = _ => true
)(implicit system: ActorSystem, ec: ExecutionContext)
    extends FutureRetryStrategy
    with Logging {

  override def retry[T](producer: => Future[T]): Future[T] =
    recursiveRetry(producer)

  protected def recursiveRetry[T](producer: => Future[T],
                                  delay: FiniteDuration = firstDelay,
                                  count: Long = 1L): Future[T] = {
    producer.recoverWith {
      case t if shouldRetry(t) =>
        if (count <= maxCount) {
          after(delay, system.scheduler)(Future.successful(true)).flatMap { x =>
            val multiple = math.pow(factor, count.toDouble).toLong
            val computedDelay = firstDelay * multiple
            val nextDelay =
              if (computedDelay > maxDelay) maxDelay else computedDelay
            recursiveRetry(producer, nextDelay, count + 1L)
          }
        } else {
          Future.failed(t)
        }
      case t =>
        Future.failed(t)
    }
  }
}

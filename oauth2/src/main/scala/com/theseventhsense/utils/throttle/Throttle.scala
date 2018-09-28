package com.theseventhsense.utils.throttle

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.after
import cats.implicits._

import com.theseventhsense.utils.logging.{LogContext, Logging}
import com.theseventhsense.utils.throttle.models.RateBucket
import com.theseventhsense.utils.types.SSDateTime
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.defaults._
import io.circe.syntax._
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * A ThrottleCalculator produces a duration for each time it is called. It is
  * expected to leverage some heuristic, such as a Rate Limit, to determine
  * the duration.
  */
trait ThrottleCalculator {
  def duration(now: SSDateTime.Instant = SSDateTime.now): FiniteDuration
}

case class Rate(count: Long, duration: FiniteDuration) {
  def countPerSec: Double = {
    count.toDouble / duration.toSeconds.toDouble
  }
}

object Rate {

  def from(count: Long, durationLength: Long, durationUnit: String): Rate = {
    val unit = TimeUnit.valueOf(durationUnit)
    val duration = new FiniteDuration(durationLength, unit)
    apply(count, duration)
  }

  implicit val decoder: Decoder[Rate] = new Decoder[Rate] {
    override def apply(c: HCursor): Result[Rate] =
      for {
        count <- c.get[Long]("count")
        durationLength <- c.get[Long]("durationLength")
        durationUnit <- c.get[String]("durationUnit")
      } yield Rate.from(count, durationLength, durationUnit)
  }

  implicit val encoder: Encoder[Rate] = new Encoder[Rate] {
    override def apply(r: Rate): Json =
      Json.obj(
        "count" -> r.count.asJson,
        "durationLength" -> r.duration.length.asJson,
        "durationUnit" -> r.duration.unit.toString.asJson
      )
  }

}

object RateThrottleCalculator {
  implicit def dateTimeOrdering: Ordering[DateTime] =
    Ordering.fromLessThan(_ isBefore _)

  /**
    * Construct a new RateThrottleCalculator from e list of rate definitions and
    * an optional bucket json. If the @bucketJson is provided, it will only be used
    * if it deserializes correctly and it has the same rate definitions as
    * rates.
    *
    * @param rates The rates to use for the new calculator
    * @param bucketJson Previously stored state to use if possible
    * @return
    */
  def apply(rates: Seq[Rate],
            bucketJson: Option[String] = None): RateThrottleCalculator = {
    val calc = for {
      json <- bucketJson
      calc <- fromJson(json).toOption
      validCalc <- if (calc.ratesMatch(rates)) {
        Some(calc)
      } else {
        None
      }
    } yield calc

    calc.getOrElse {
      val buckets = rates.map { rate =>
        RateBucket(
          rate,
          0,
          SSDateTime.Instant.parse("1900-01-01T00:00:00Z").toOption.get
        )
      }
      new RateThrottleCalculator(buckets)
    }
  }

  /**
    * Load from a json object by deserializing the buckets.
    *
    * @param json The JsValue to deserialize
    * @return a JsResult containing the deserialized calculator
    */
  def fromJson(json: String): Either[Error, RateThrottleCalculator] = {
    parser.decode[Seq[RateBucket]](json).map { buckets =>
      new RateThrottleCalculator(buckets)
    }
  }
}

/**
  * Calculate throttle durations using a bucket of rates. All rates will be applied
  * to every request and the maximum delay used, ensuring that all rate conditions
  * are met.
  *
  * @param buckets
  */
class RateThrottleCalculator(buckets: Seq[RateBucket])
    extends ThrottleCalculator {
  private var now: SSDateTime.Instant = SSDateTime.now

  /**
    * Serialize this calculator to json
    *
    * @return The json representation of the current buckets state.
    */
  def toJson: String = synchronized {
    buckets.asJson.spaces2
  }

  /**
    * Check if the provided rates match the ones already in use
    *
    * @param rates
    * @return
    */
  def ratesMatch(rates: Seq[Rate]): Boolean = synchronized {
    rates.toSet == buckets.map(_.rate).toSet
  }

  /**
    * Return the start time of the earliest bucket.
    *
    * @return
    */
  def start: SSDateTime.Instant = synchronized {
    buckets.map(_.start).min
  }

  /**
    * Compute the delay duration to use for the next request by incrementing each
    * rate bucket and determine the maximum delay.
    *
    * @param now
    * @return
    */
  def duration(now: SSDateTime.Instant = SSDateTime.now): FiniteDuration =
    synchronized {
      this.now = now
      val durations: Seq[FiniteDuration] = buckets.map { bucket =>
        bucket.incrementedDuration(now)
      }
      if (durations.nonEmpty) {
        durations.max
      } else {
        0.seconds
      }
    }
}

/**
  * Leveraging @calculator to determine the delay to apply to each request, Throttle
  * uses the akka "after" pattern provided by @system to delay the return of every
  * future pass to executeThrottled.
  *
  * This delay mechanism does not actually defer the execution of the future,
  * but instead defers the return of the response. For any more or less serial
  * operation this is equivalent, but for the case where an unbounded number of
  * parralel operations are started, it is very likely it will be insufficient.
  *
  * @param calculator
  * @param system
  * @param ec
  */
class Throttle(calculator: ThrottleCalculator)(implicit system: ActorSystem) extends Logging {
  def executeThrottled[T](work: => Future[T])(
    implicit ec: ExecutionContext,
    lc: LogContext
  ): Future[T] = {
    val duration = calculator.duration()
    logger.trace(s"Throttle duration: $duration")
    after(duration, system.scheduler)(Future.successful(true))
      .flatMap(_ => work)
  }
}

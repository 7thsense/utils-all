package com.theseventhsense.utils.throttle.models

import com.theseventhsense.utils.throttle.Rate
import com.theseventhsense.utils.types.SSDateTime
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.defaults._
import com.theseventhsense.udatetime.UDateTimeCodecs._

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

/**
  * A rate bucket wraps up an @rate, a @count, and the @start time of the first
  * item in the bucket. It uses these to calculate the rate needed or delay needed
  * to keep future requests within the rate.
  *
  * If a request is made after the current window, the window is reset.
  *
  * @param rate The rate to apply
  * @param count The count within the current window, defaults to 0
  * @param start The beginning of the current window, defaults to now()
  */
case class RateBucket(rate: Rate,
                      var count: Long = 0,
                      var start: SSDateTime.Instant = SSDateTime.now) {
  final val BUFFER = 0.1D
  final val THRESHOLD = 0.5D

  // don't come closer than to within 10% of max
  def updatedStart(
    now: SSDateTime.Instant = SSDateTime.now
  ): SSDateTime.Instant = {
    if (elapsedSeconds(now) > rate.duration.toSeconds.toDouble) {
      count = 0
      start = now
    }
    start
  }

  def elapsedSeconds(now: SSDateTime.Instant = SSDateTime.now): Double = {
    (now.millis - start.millis).toDouble / 1000D
  }

  def remainingSeconds(now: SSDateTime.Instant = SSDateTime.now): Double = {
    (end(now).millis - now.millis).toDouble / 1000D
  }

  def end(now: SSDateTime.Instant = SSDateTime.now): SSDateTime.Instant = {
    updatedStart(now).plusMillis(rate.duration.toMillis)
  }

  def countPerSec(now: SSDateTime.Instant = SSDateTime.now): Double = {
    count.toDouble / elapsedSeconds(now)
  }

  def applies: Boolean = {
    count.toDouble > rate.count.toDouble * THRESHOLD
  }

  def throttledCountPerSec(now: SSDateTime.Instant = SSDateTime.now): Double = {
    val remainingCount: Long = rate.count - count
    remainingCount.toDouble / remainingSeconds(now) * (1D - BUFFER)
  }

  def incrementedDuration(
    now: SSDateTime.Instant = SSDateTime.now
  ): FiniteDuration = {
    count += 1
    duration(now)
  }

  /**
    * Compute a delay from a rate by taking it's reciprocal. Extracted for
    * readability and testability concerns.
    *
    * @param countPerUnit A rate, expressed in count per second
    * @return The delay
    */
  def delayFromRate(countPerUnit: Double,
                    now: SSDateTime.Instant = SSDateTime.now): Double = {
    if (countPerUnit <= 0) {
      remainingSeconds(now)
    } else {
      1D / countPerUnit
    }
  }

  def duration(now: SSDateTime.Instant = SSDateTime.now): FiniteDuration = {
    updatedStart(now)
    val throttledDelay = if (applies) {
      val countPerSec = throttledCountPerSec(now)
      delayFromRate(countPerSec)
    } else {
      0D
    }
    FiniteDuration((throttledDelay * 1000).toLong, MILLISECONDS)
  }
}

object RateBucket {
  implicit val encoder: Encoder[RateBucket] = deriveEncoder
  implicit val decoder: Decoder[RateBucket] = deriveDecoder
}

package com.theseventhsense.utils.throttle

import cats.scalatest.EitherValues

import com.theseventhsense.testing.AkkaUnitSpec
import com.theseventhsense.utils.throttle.models.RateBucket
import com.theseventhsense.utils.types.SSDateTime
import io.circe._
import io.circe.syntax._
import org.joda.time.DateTime
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import scala.concurrent.Future
import scala.concurrent.duration._

import com.theseventhsense.utils.logging.LogContext

class ThrottleSpec
    extends AkkaUnitSpec
    with ScalaFutures
    with IntegrationPatience
    with EitherValues {
  implicit val lc: LogContext = LogContext.empty
  val currentTime = SSDateTime.Instant.parse("2014-01-01T12:00:00Z").value

  "the Rate" should {
    val rate = Rate(1, 1.seconds)
    val rateJson =
      parser.parse("""
                     |{"count":1,"durationLength":1,"durationUnit":"SECONDS"}
      """.stripMargin).value
    "serialize to json" in {
      rate.asJson mustEqual rateJson
    }
    "deserialize from json" in {
      rateJson.as[Rate].value mustEqual rate
    }
  }
  "the Throttle" should {
    class OneSecondDelay extends ThrottleCalculator {
      def duration(now: SSDateTime.Instant = SSDateTime.now): FiniteDuration =
        1.second
    }
    val throttleClient = new Throttle(new OneSecondDelay)
    val f = Future {
      "result"
    }
    "run normally" in {
      val start = DateTime.now()
      val result = f.futureValue
      val elapsed = DateTime.now.getMillis - start.getMillis
      elapsed must be < 999L
      result mustEqual "result"
    }
    "delay" in {
      val start = DateTime.now()
      val result = throttleClient.executeThrottled(f).futureValue
      val elapsed = DateTime.now.getMillis - start.getMillis
      elapsed must be >= 1000L
      result mustEqual "result"
    }
  }
  "the RateBucket" should {
    val rate = Rate(10, 1.second)
    val bucket = RateBucket(rate, 0, currentTime)
    "only apply if over THRESHOLD of the count is used" in {
      bucket.count = 1
      bucket.applies mustEqual false
      bucket.count = 2
      bucket.applies mustEqual true
    }
    "compute a rate based on the remaining time and a buffer" in {
      bucket.count = 6
      val countPerSec =
        bucket.throttledCountPerSec(currentTime.plusMillis(600L))
      countPerSec mustEqual 9.0 // 10 * 90%
    }
    "compute a delay from a rate" in {
      bucket.delayFromRate(0.5) mustEqual 2.0
      bucket.delayFromRate(1.0) mustEqual 1.0
      bucket.delayFromRate(2.0) mustEqual 0.5
    }
    "calculate elapsed time" in {
      bucket.elapsedSeconds(currentTime.plusMillis(100L)) mustEqual 0.1D
    }
    "compute the end of the rate period" in {
      val end = currentTime.plusMillis(rate.duration.toMillis)
      bucket.end(currentTime) mustEqual end
    }
  }
  "the RateThrottleCalculator" when {
    "configured with a single rate" should {
      val rate = Rate(10, 1.second)
      val calc = RateThrottleCalculator(Seq(rate))
      "initialize the ratebuckets with the first date it sees" in {
        calc.duration(currentTime) mustEqual 0.seconds
        calc.start mustEqual currentTime
      }
      "avoid delaying requests if they are below THRESHOLD" in {
        calc.duration(currentTime.plusMillis(20)) mustEqual 0.seconds
      }
      "requests above the minimum count should be delayed" in {
        calc.duration(currentTime.plusMillis(30)) mustEqual 134.millis
      }
      "the last request in the rate period should be deferred into the next period" in {
        val millisToAdd = 200 + 222 + 214 + 202 + 180
        millisToAdd mustEqual 1018
        val futureTime = currentTime.plusMillis(millisToAdd.toLong)
        calc.duration(futureTime) mustEqual 0.millis
      }
      "ensure delayed requests exceed the rate period by at least BUFFER percent" in {
        200 + 222 + 214 + 202 + 180 mustEqual 1018
      }
    }
  }
}

package com.theseventhsense.utils.retry

import com.theseventhsense.testing.AkkaUnitSpec
import com.theseventhsense.utils.types.SSDateTime

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by erik on 3/3/16.
  */
class GenericBackOffRetryStrategySpec extends AkkaUnitSpec {
  class FailingFuture(whenToSucceed: Int = 0) {
    val startTime = SSDateTime.now
    var lastCall = SSDateTime.now
    var count = 0
    def doWork: Future[Boolean] = {
      lastCall = SSDateTime.now
      if (count >= whenToSucceed) {
        Future.successful(true)
      } else {
        count += 1
        Future.failed(new RuntimeException("foo"))
      }
    }

    def elapsed: FiniteDuration =
      Duration(lastCall.millis - startTime.millis, MILLISECONDS)
  }
  "the GenericBackOffRetryStrategy" ignore {
    "be able to return values that don't fail immediately" in {
      val strategy = new FutureBackOffRetryStrategy(10.millis, maxCount = 0)
      val worker = new FailingFuture()
      val result = strategy.retry(worker.doWork)
      Await.result(result, 100.millis) mustEqual true
      worker.elapsed.toMillis must be < 10L
    }
    "fail to handle a single failure when 0 retries are configured" in {
      val strategy = new FutureBackOffRetryStrategy(10.millis, maxCount = 0)
      val worker = new FailingFuture(1)
      val result = strategy.retry(worker.doWork)
      result.failed.futureValue mustBe a[RuntimeException]
    }
    "be able to handle a single failure with a single retry" in {
      val strategy = new FutureBackOffRetryStrategy(10.millis, maxCount = 1)
      val worker = new FailingFuture(1)
      val result = strategy.retry(worker.doWork)
      Await.result(result, 100.millis) mustEqual true
      worker.elapsed.toMillis must be >= 10L
      worker.elapsed.toMillis must be < 50L
    }
    "fail to handle two failures with a single retry" in {
      val strategy = new FutureBackOffRetryStrategy(10.millis, maxCount = 1)
      val worker = new FailingFuture(2)
      val result = strategy.retry(worker.doWork)
      result.failed.futureValue mustBe a[RuntimeException]
    }
    "be able to handle two failures with two retries" in {
      val strategy = new FutureBackOffRetryStrategy(10.millis, maxCount = 2)
      val worker = new FailingFuture(2)
      val result = strategy.retry(worker.doWork)
      Await.result(result, 100.millis) mustEqual true
      worker.elapsed.toMillis must be >= 20L
      worker.elapsed.toMillis must be < 90L
    }
    "fail to handle three failures with a two retries" in {
      val strategy = new FutureBackOffRetryStrategy(10.millis, maxCount = 2)
      val worker = new FailingFuture(3)
      val result = strategy.retry(worker.doWork)
      result.failed.futureValue mustBe a[RuntimeException]
    }
    "be able to handle three failures with three retries" in {
      val strategy = new FutureBackOffRetryStrategy(10.millis, maxCount = 3)
      val worker = new FailingFuture(3)
      val result = strategy.retry(worker.doWork)
      Await.result(result, 200.millis) mustEqual true
      worker.elapsed.toMillis must be <= (10L * math.pow(2L, 2)).toLong
      worker.elapsed.toMillis must be < (10L * math.pow(2L, 4)).toLong
    }
    "not exceed the maximum delay" in {
      val maxDelay = 25.millis
      val strategy = new FutureBackOffRetryStrategy(
        10.millis,
        maxCount = 10,
        maxDelay = maxDelay
      )
      val worker = new FailingFuture(5)
      val result = strategy.retry(worker.doWork)
      Await.result(result, 500.millis) mustEqual true
      worker.count mustEqual 5
      worker.elapsed.toMillis must be >= (maxDelay * 5).toMillis
      worker.elapsed.toMillis must be < (maxDelay * 5 * 2).toMillis
    }

  }
}

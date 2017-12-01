package ss.utils.datetime

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

/**
  * Created by erik on 7/19/16.
  */
class InstantSpec extends WordSpec with MustMatchers with OptionValues {
  val millis = 1469030460000L
  val utcDateTimeStr = "2016-07-20T16:01:00Z"
  val offsetDateTimeStr = "2016-07-20T12:01:00-04:00"
  val zonedDateTimeStr = s"$offsetDateTimeStr[America/New_York]"
  "the Instant companion object" should {
    "parse a string containing millis since epoch" in {
      val now = SSDateTime.Instant.parse(millis.toString).toOption.value
      now.millis mustEqual millis
    }
    "parse a date with with a utc offset" in {
      val now = SSDateTime.Instant.parse(utcDateTimeStr).toOption.value
      now.millis mustEqual millis

    }
    "parse a date with an offset" in {
      val now = SSDateTime.Instant.parse(offsetDateTimeStr).toOption.value
      now.millis mustEqual millis
    }
    "parse a date with a zone" in {
      val nowWithZone = SSDateTime.Instant.parse(zonedDateTimeStr).toOption.value
      nowWithZone.millis mustEqual millis
    }
  }
}

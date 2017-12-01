package ss.utils.datetime

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime
import com.theseventhsense.utils.types.SSDateTime.DateTime.Format
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

/**
  * Created by erik on 7/19/16.
  */
class DateTimeSpec extends WordSpec with MustMatchers with OptionValues {
  val offsetDateTimeStr = "2016-07-20T12:01:00-04:00"
  val zonedDateTimeStr = s"$offsetDateTimeStr[America/New_York]"
  lazy val now = SSDateTime.DateTime.parse(offsetDateTimeStr).toOption.value
  lazy val nowWithZone = SSDateTime.DateTime.parse(zonedDateTimeStr).toOption.value
  "the DateTime class" should {
    "parse a date with a zone" in {
      nowWithZone.instant.millis mustEqual 1469030460000L
    }
    "format as a zoned IsoZonedDateTime" in {
      nowWithZone.format(Format.IsoZonedDateTime) mustEqual zonedDateTimeStr
    }
    "parse a date with an offset" in {
      now.instant.millis mustEqual 1469030460000L
    }
    "format as a IsoZonedDateTime" in {
      now.format(Format.IsoZonedDateTime) mustEqual offsetDateTimeStr
    }
    "format as a Year" in {
      now.format(Format.Year) mustEqual "2016"
    }
    "format as a YearMonthDay" in {
      now.format(Format.YearMonthDay) mustEqual "2016-07-20"
    }
    "format as a HourAP" in {
      now.format(Format.HourAP) mustEqual "12p"
    }
    "format as a HourMinuteAmPm" in {
      now.format(Format.HourMinuteAmPm) mustEqual "12:01 PM"
    }
  }

}

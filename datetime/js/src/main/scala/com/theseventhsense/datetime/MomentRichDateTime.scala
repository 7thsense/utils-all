package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime.DateTime.{Format, ParseError}
import com.theseventhsense.utils.types.SSDateTime._

/**
  * Created by erik on 6/15/16.
  */
class MomentRichDateTime(dateTime: DateTime)
    extends AbstractRichDateTime(dateTime) {
  override def withZoneSameInstant(timeZone: TimeZone): DateTime = ???

  override def withMillisOfSecond(millisOfSecond: Int): DateTime = ???

  override def withMinuteOfHour(minuteOfHour: Int): DateTime = ???

  override def withDayOfWeek(dayOfWeek: DayOfWeek): DateTime = ???

  override def dayOfWeek: DayOfWeek = ???

  override def dayOfYear: Int = ???

  override def secondOfDay: Int = ???

  override def minuteOfHour: Int = ???

  override def hourOfDay: HourOfDay = ???

  override def withHourNumOfDay(hourOfDay: Int): DateTime = ???

  override def withNextEvenHour: DateTime = ???

  override def withRoundedMinute: DateTime = ???

  override def dayOfMonth: DayOfMonth = ???

  override def month: Month = ???

  override def year: Year = ???

  override def plusMonths(weeks: Int): DateTime = ???

  override def minusMonths(weeks: Int): DateTime = ???

  override def plusYears(year: Int): DateTime = ???

  override def minusYears(years: Int): DateTime = ???

  override def atStartOfDay: DateTime = ???

  override def withDayNumOfWeek(dayOfWeekNum: Int): DateTime = ???

  override def withSecondOfMinute(secondOfMinute: Int): DateTime = ???

  override def withZoneSameLocal(timeZone: TimeZone): DateTime = ???

  override def toIsoString: String = ???

  override def format(format: Format): String = ???
}

class MomentRichDateTimeOps extends AbstractRichDateTimeOps {
  override def parse(s: String): Either[ParseError, DateTime] =
    SSDateTimeParser.parse(s)
}

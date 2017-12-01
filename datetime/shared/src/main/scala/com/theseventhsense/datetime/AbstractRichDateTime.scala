package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime
import com.theseventhsense.utils.types.SSDateTime.DateTime.Format
import com.theseventhsense.utils.types.SSDateTime.{DateTime, DayOfWeek, HourOfDay, TimeZone}

/**
  * Created by erik on 6/15/16.
  */
abstract class AbstractRichDateTime(dateTime: DateTime) extends Serializable {
  def withZoneSameInstant(timeZone: TimeZone): DateTime
  def withZoneSameLocal(timeZone: TimeZone): DateTime
  def withMillisOfSecond(millisOfSecond: Int): DateTime
  def withSecondOfMinute(secondOfMinute: Int): DateTime
  def withMinuteOfHour(minuteOfHour: Int): DateTime
  def withHourNumOfDay(hourOfDay: Int): DateTime
  def withHourOfDay(hourOfDay: HourOfDay): DateTime =
    withHourNumOfDay(hourOfDay.num)
  def withDayNumOfWeek(dayOfWeekNum: Int): DateTime
  def withDayOfWeek(dayOfWeek: DayOfWeek): DateTime
  def withNextEvenHour: DateTime
  def withRoundedMinute: DateTime
  def plusMonths(weeks: Int): DateTime
  def minusMonths(weeks: Int): DateTime
  def plusYears(year: Int): DateTime
  def minusYears(years: Int): DateTime
  def format(format: Format): String
  def atStartOfDay: DateTime
  def secondOfDay: Int
  def minuteOfHour: Int
  def hourOfDay: HourOfDay
  def dayOfMonth: SSDateTime.DayOfMonth
  def month: SSDateTime.Month
  def dayOfYear: Int
  def dayOfWeek: DayOfWeek
  def year: SSDateTime.Year
  def toIsoString: String
}

abstract class AbstractRichDateTimeOps extends Serializable {
  def parse(s: String): Either[DateTime.ParseError, DateTime]
}

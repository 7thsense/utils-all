package com.theseventhsense.datetime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalAdjusters}

import com.theseventhsense.utils.types.SSDateTime
import com.theseventhsense.utils.types.SSDateTime.DateTime.Format
import com.theseventhsense.utils.types.SSDateTime._

/**
  * Created by erik on 6/15/16.
  */
class JavaTimeRichDateTime(dateTime: DateTime)
    extends AbstractRichDateTime(dateTime)
    with JavaTimeInstantImplicits
    with JavaTimeTimeZoneImplicits
    with JavaTimeImplicits {

  lazy val asJavaTime: ZonedDateTime =
    ZonedDateTime.ofInstant(
      dateTime.instant.asJavaTime,
      dateTime.zone.asJavaTime
    )

  override def withZoneSameInstant(timeZone: TimeZone): DateTime =
    asJavaTime.withZoneSameInstant(timeZone.asJavaTime).asU

  override def withZoneSameLocal(timeZone: TimeZone): DateTime =
    asJavaTime.withZoneSameLocal(timeZone.asJavaTime).asU

  override def withMillisOfSecond(millisOfSecond: Int): DateTime =
    asJavaTime.withNano(millisOfSecond * 1000).asU

  override def withMinuteOfHour(minuteOfHour: Int): DateTime =
    asJavaTime.withMinute(minuteOfHour).asU

  override def withDayOfWeek(dayOfWeek: DayOfWeek): DateTime = {
    withDayNumOfWeek(dayOfWeek.isoNumber)
  }

  override def dayOfYear: Int = asJavaTime.getDayOfYear

  override def dayOfWeek: DayOfWeek =
    DayOfWeek.from(asJavaTime.getDayOfWeek.getValue).get

  override def secondOfDay: Int =
    asJavaTime.getSecond + minuteOfHour * 60 + hourOfDay.num * 60 * 60

  override def minuteOfHour: Int = asJavaTime.getMinute

  override def hourOfDay: HourOfDay = HourOfDay.from(asJavaTime.getHour).get

  override def withHourNumOfDay(hourOfDay: Int): DateTime =
    asJavaTime.withHour(hourOfDay).asU

  override def withNextEvenHour: DateTime =
    asJavaTime.truncatedTo(ChronoUnit.HOURS).plusHours(1).asU

  override def withRoundedMinute: DateTime =
    asJavaTime.truncatedTo(ChronoUnit.MINUTES).asU

  override def dayOfMonth: DayOfMonth =
    SSDateTime.DayOfMonth.from(asJavaTime.getDayOfMonth).get

  override def month: Month =
    SSDateTime.Month.from(asJavaTime.getMonthValue).get

  override def year: SSDateTime.Year = SSDateTime.Year(asJavaTime.getYear)

  override def plusMonths(months: Int): DateTime =
    asJavaTime.plusMonths(months.toLong).asU
  override def minusMonths(months: Int): DateTime =
    asJavaTime.minusMonths(months.toLong).asU
  override def plusYears(years: Int): DateTime = asJavaTime.plusYears(years.toLong).asU
  override def minusYears(years: Int): DateTime =
    asJavaTime.minusYears(years.toLong).asU

  override def atStartOfDay: DateTime =
    asJavaTime.toLocalDate.atStartOfDay(dateTime.zone.asJavaTime).asU

  override def withDayNumOfWeek(dayOfWeekNum: Int): DateTime = {
    val dayOfWeek = java.time.DayOfWeek.of(dayOfWeekNum)
    if (dayOfWeekNum > asJavaTime.getDayOfWeek.getValue) {
      asJavaTime.`with`(TemporalAdjusters.nextOrSame(dayOfWeek)).asU
    } else {
      asJavaTime.`with`(TemporalAdjusters.previousOrSame(dayOfWeek)).asU
    }
  }

  override def withSecondOfMinute(secondOfMinute: Int): DateTime =
    asJavaTime.withSecond(secondOfMinute).asU

  override def toIsoString: String = format(Format.IsoZonedDateTime)

  override def format(format: Format): String = format match {
    case Format.HourAP =>
      asJavaTime
        .format(DateTimeFormatter.ofPattern("ha"))
        .replace("AM", "a")
        .replace("PM", "p")
    case Format.HourMinuteAmPm =>
      asJavaTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
    case Format.Year =>
      asJavaTime.format(DateTimeFormatter.ofPattern("YYYY"))
    case Format.YearMonth =>
      asJavaTime.format(DateTimeFormatter.ofPattern("YYYY-MM"))
    case Format.YearMonthDay =>
      asJavaTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
    case Format.IsoZonedDateTime =>
      asJavaTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
  }
}

class JavaTimeRichDateTimeOps
    extends AbstractRichDateTimeOps
    with JavaTimeImplicits {
  override def parse(s: String): Either[DateTime.ParseError, DateTime] =
    SSDateTimeParser.parse(s)
}

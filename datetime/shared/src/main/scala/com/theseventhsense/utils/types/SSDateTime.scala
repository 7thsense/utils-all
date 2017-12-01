package com.theseventhsense.utils.types

import cats.implicits._
import java.util
import java.util.Date

import com.theseventhsense.datetime._

import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.util.Try

/**
  * Created by erik on 11/11/15.
  */
object SSDateTime {
  val dateTimeOps: AbstractRichDateTimeOps = RichDateTime
  val timeZoneOps: AbstractRichTimeZoneOps = RichTimeZone
  val instantOps: AbstractRichInstantOps = RichInstant

  def now: Instant = Instant.now

  def parse(s: String): Either[DateTime.ParseError, DateTime] =
    dateTimeOps.parse(s)

  case class Instant(millis: Long) extends Comparable[Instant] {
    def asDate: Date = new Date(millis)

    def isEqual(other: Instant): Boolean = millis == other.millis

    def isBefore(other: Instant): Boolean = millis < other.millis

    def isAfter(other: Instant): Boolean = millis > other.millis

    def >(other: Instant): Boolean = millis > other.millis

    def <(other: Instant): Boolean = millis < other.millis

    def >=(other: Instant): Boolean = millis >= other.millis

    def <=(other: Instant): Boolean = millis <= other.millis

    def +(duration: Duration): Instant = Instant(millis + duration.toMillis)

    //scalastyle: ignore
    def -(duration: Duration): Instant = Instant(millis - duration.toMillis)

    //scalastyle: ignore
    def plus(duration: Duration): Instant = Instant(millis + duration.toMillis)

    def minus(duration: Duration): Instant =
      Instant(millis - duration.toMillis)

    def plusMillis(m: Long): Instant = Instant(millis + m)

    def minusMillis(m: Long): Instant = Instant(millis - m)

    def plusSeconds(seconds: Long): Instant = plusMillis(seconds * 1000)

    def minusSeconds(seconds: Long): Instant = minusMillis(seconds * 1000)

    def plusMinutes(minutes: Int): Instant = plusSeconds(minutes.toLong * 60)

    def minusMinutes(minutes: Int): Instant = minusSeconds(minutes.toLong * 60)

    def plusHours(hours: Int): Instant = plusMinutes(hours * 60)

    def minusHours(hours: Int): Instant = minusMinutes(hours * 60)

    def plusDays(days: Int): Instant = plusHours(days * 24)

    def minusDays(days: Int): Instant = minusHours(days * 24)

    def plusWeeks(weeks: Int): Instant = plusDays(weeks * 7)

    def minusWeeks(weeks: Int): Instant = minusDays(weeks * 7)

    def plusMonths(months: Int): Instant =
      DateTime(this).plusMonths(months).instant

    def minusMonths(months: Int): Instant =
      DateTime(this).minusMonths(months).instant

    def plusYears(years: Int): Instant = DateTime(this).plusYears(years).instant

    def minusYears(years: Int): Instant =
      DateTime(this).minusYears(years).instant

    def withRoundedMinute: Instant = DateTime(this).withRoundedMinute.instant

    def inZone(timeZone: TimeZone): DateTime = DateTime.apply(this, timeZone)

    def inUTC: DateTime = this.inZone(TimeZone.UTC)

    def calendar: String = this.calendarInZone(SSDateTime.TimeZone.Default)

    override def toString: String = this.asIsoString

    override def compareTo(o: Instant): Int = millis.compareTo(o.millis)
  }

  object Instant {

    sealed trait ParseError

    object ParseError {

      case class Unknown(message: String) extends ParseError
    }

    implicit def enrichInstant(instant: Instant): AbstractRichInstant =
      new RichInstant(instant)

    implicit val ordering: Ordering[Instant] = Ordering.by { instant: Instant =>
      instant.millis
    }

    def parse(s: String): Either[ParseError, Instant] = instantOps.parse(s)

    def parseAsLocal(s: String): Either[ParseError, Instant] =
      instantOps.parseLocalAsUTC(s)

    def apply(date: Date): Instant = Instant(date.getTime)

    def now: Instant = apply(System.currentTimeMillis())

    val Max = apply(Long.MaxValue)
    val Min = apply(Long.MinValue)
  }

  case class DateTime(instant: Instant, zone: TimeZone = TimeZone.UTC)
      extends Comparable[DateTime] {
    override def compareTo(o: DateTime): Int = instant.compareTo(o.instant)

    def isEqual(dateTime: DateTime): Boolean = this == dateTime

    def isAfter(dateTime: DateTime): Boolean =
      instant.isAfter(dateTime.instant)

    def isBefore(dateTime: DateTime): Boolean =
      instant.isBefore(dateTime.instant)

    def plusHours(days: Int): DateTime =
      DateTime(instant.plusHours(days), zone)

    def minusHours(days: Int): DateTime =
      DateTime(instant.minusHours(days), zone)

    def plusDays(days: Int): DateTime = DateTime(instant.plusDays(days), zone)

    def minusDays(days: Int): DateTime =
      DateTime(instant.minusDays(days), zone)

    def plusWeeks(weeks: Int): DateTime =
      DateTime(instant.plusWeeks(weeks), zone)

    def minusWeeks(weeks: Int): DateTime =
      DateTime(instant.minusWeeks(weeks), zone)

    override def toString: String = DateTime.enrichDateTime(this).toIsoString
  }

  object DateTime {
    sealed abstract class Format extends Product with Serializable

    object Format {
      case object Year extends Format
      case object YearMonth extends Format
      case object YearMonthDay extends Format
      case object HourAP extends Format
      case object HourMinuteAmPm extends Format
      case object IsoZonedDateTime extends Format
    }

    sealed abstract class ParseError extends Product with Serializable

    object ParseError {

      case object NotImplemented extends ParseError

      case class Unknown(message: String) extends ParseError
    }

    implicit def enrichDateTime(dateTime: DateTime): AbstractRichDateTime =
      new RichDateTime(dateTime)

    def fromMillis(millis: Long, zone: TimeZone): DateTime =
      DateTime(Instant(millis), zone)

    def parse(s: String): Either[ParseError, DateTime] = dateTimeOps.parse(s)
  }

  trait TimeZone extends Serializable {
    def name: String

    def asUtilTimeZone: util.TimeZone = {
      Option(util.TimeZone.getTimeZone(name))
        .getOrElse(util.TimeZone.getDefault)
    }

    override def toString: String = name

    override def hashCode: Int = name.hashCode

    override def equals(obj: Any): Boolean = obj match {
      case timeZone: TimeZone =>
        name == timeZone.name
      case _ =>
        false
    }
  }

  case class CustomTimeZone(name: String) extends TimeZone {
    override def toString: String = s"$name (Custom)"
  }

  case class DisplayTimeZone(name: String, displayName: String, offset: Int, offsetId: String)
      extends TimeZone

  sealed trait KnownTimeZone extends TimeZone {
    def knownName: String

    def knownAliases: Seq[String] = Seq.empty

    override def toString: String = s"$knownName"
  }

  object KnownTimeZone {
    def from(s: String, when: SSDateTime.Instant = now): Option[TimeZone] = {
      val unknownTimeZone =
        KnownTimeZone.apply(s).orElse(TimeZone.parse(s).toOption)
      unknownTimeZone.flatMap { zone =>
        TimeZone.all.find(
          tz => tz.offsetSecondsAt(when) == zone.offsetSecondsAt(when)
        )
      }
    }

    def apply(s: String): Option[TimeZone] =
      TimeZone.all
        .find(_.name == s)
        .orElse(TimeZone.all.find(_.knownName == s))
        .orElse(TimeZone.all.find(_.knownAliases.contains(s)))
  }

  object TimeZone {

    sealed abstract class ParseError extends Product with Serializable

    object ParseError {

      case object NotImplemented extends ParseError

      case object Unknown extends ParseError
    }

    implicit def enrichTimeZone(timeZone: TimeZone): AbstractRichTimeZone =
      new RichTimeZone(timeZone)

    case object UTC extends KnownTimeZone {
      override val name = "UTC"
      override val knownName = "Universal Time"
      override val knownAliases = Seq("Z", "z", "GMT")
    }

    object US {

      case object Eastern extends KnownTimeZone {
        override val name = "America/New_York"
        override val knownName = "US/Eastern"
      }

      case object Central extends KnownTimeZone {
        val name = "America/Chicago"
        override val knownName = "US/Central"
      }

      case object Mountain extends KnownTimeZone {
        val name = "America/Denver"
        override val knownName = "US/Mountain"
      }

      case object Pacific extends KnownTimeZone {
        val name = "America/Los_Angeles"
        override val knownName = "US/Pacific"
      }

      val all = Seq(Eastern, Central, Mountain, Pacific)
    }

    object Europe {

      case object Western extends KnownTimeZone {
        val name = "Europe/Berlin"
        override val knownName = "Europe/Western"
      }

      case object Eastern extends KnownTimeZone {
        val name = "Europe/Bucharest"
        override val knownName = "Europe/Eastern"
      }

      case object Central extends KnownTimeZone {
        val name = "Europe/London"
        override val knownName = "Europe/Central"
      }

      val all = Seq(Eastern, Central, Western)
    }

    object Australia {

      case object Southern extends KnownTimeZone {
        val name = "Australia/Adelaide"
        override val knownName = "Australia/Southern"
      }

      val all = Seq(Southern)
    }

    object Pacific {

      case object Auckland extends KnownTimeZone {
        val name = "Pacific/Auckland"
        override val knownName = "Pacific/Auckland"
      }

      val all = Seq(Auckland)
    }

    val Default = US.Eastern

    val all = Seq(UTC) ++ US.all ++ Europe.all ++ Australia.all ++ Pacific.all

    def parse(s: String): Either[ParseError, TimeZone] = timeZoneOps.parse(s)

    def from(s: String): TimeZone =
      KnownTimeZone
        .apply(s)
        .orElse(parse(s).toOption)
        .getOrElse(CustomTimeZone(s))
  }

  sealed trait DayOfWeek extends Product with Serializable {
    def isoNumber: Int

    def shortText: String
  }

  object DayOfWeek {
    implicit val dayOfWeekOrdering: Ordering[DayOfWeek] =
      Ordering.by(_.isoNumber)

    val First = Monday

    case object Monday extends DayOfWeek {
      override val isoNumber = 1
      override val shortText = "Mon"
    }

    case object Tuesday extends DayOfWeek {
      override val isoNumber = 2
      override val shortText = "Tue"
    }

    case object Wednesday extends DayOfWeek {
      override val isoNumber = 3
      override val shortText = "Wed"
    }

    case object Thursday extends DayOfWeek {
      override val isoNumber = 4
      override val shortText = "Thu"
    }

    case object Friday extends DayOfWeek {
      override val isoNumber = 5
      override val shortText = "Fri"
    }

    case object Saturday extends DayOfWeek {
      override val isoNumber = 6
      override val shortText = "Sat"
    }

    case object Sunday extends DayOfWeek {
      override val isoNumber = 7
      override val shortText = "Sun"
    }

    val all =
      Seq(Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)

    def from(isoNumber: Int): Option[DayOfWeek] =
      all.find(_.isoNumber == isoNumber)

    def fromString(s: String): Option[DayOfWeek] =
      Try(s.toInt).toOption
        .flatMap(isoNumber => all.find(d => d.isoNumber == isoNumber))
        .orElse(all.find(d => d.shortText == s))
  }

  sealed trait HourOfDay extends Product with Serializable {
    def num: Int

    override def toString: String = formatHourShort

    def formatHourShort: String = {
      val hour = this.num
      if (hour == 0) {
        "mid"
      } else if (hour < 12) {
        s"${hour}a"
      } else if (hour == 12) {
        "noon"
      } else if (hour > 12) {
        s"${hour - 12}p"
      } else {
        "inv"
      }
    }

    def formatHour: String = {
      val hour = this.num
      if (hour == 0) {
        "12 Midnight"
      } else if (hour < 12) {
        s"${hour} AM"
      } else if (hour == 12) {
        "12 Noon"
      } else if (hour > 12) {
        s"${hour - 12} PM"
      } else {
        "inv"
      }
    }
  }

  object HourOfDay {
    implicit val ordering: Ordering[HourOfDay] = Ordering.by(_.num)

    case object Hour00 extends HourOfDay {
      val num = 0
    }

    case object Hour01 extends HourOfDay {
      val num = 1
    }

    case object Hour02 extends HourOfDay {
      val num = 2
    }

    case object Hour03 extends HourOfDay {
      val num = 3
    }

    case object Hour04 extends HourOfDay {
      val num = 4
    }

    case object Hour05 extends HourOfDay {
      val num = 5
    }

    case object Hour06 extends HourOfDay {
      val num = 6
    }

    case object Hour07 extends HourOfDay {
      val num = 7
    }

    case object Hour08 extends HourOfDay {
      val num = 8
    }

    case object Hour09 extends HourOfDay {
      val num = 9
    }

    case object Hour10 extends HourOfDay {
      val num = 10
    }

    case object Hour11 extends HourOfDay {
      val num = 11
    }

    case object Hour12 extends HourOfDay {
      val num = 12
    }

    case object Hour13 extends HourOfDay {
      val num = 13
    }

    case object Hour14 extends HourOfDay {
      val num = 14
    }

    case object Hour15 extends HourOfDay {
      val num = 15
    }

    case object Hour16 extends HourOfDay {
      val num = 16
    }

    case object Hour17 extends HourOfDay {
      val num = 17
    }

    case object Hour18 extends HourOfDay {
      val num = 18
    }

    case object Hour19 extends HourOfDay {
      val num = 19
    }

    case object Hour20 extends HourOfDay {
      val num = 20
    }

    case object Hour21 extends HourOfDay {
      val num = 21
    }

    case object Hour22 extends HourOfDay {
      val num = 22
    }

    case object Hour23 extends HourOfDay {
      val num = 23
    }

    val all = Seq(
      Hour00,
      Hour01,
      Hour02,
      Hour03,
      Hour04,
      Hour05,
      Hour06,
      Hour07,
      Hour08,
      Hour09,
      Hour10,
      Hour11,
      Hour12,
      Hour13,
      Hour14,
      Hour15,
      Hour16,
      Hour17,
      Hour18,
      Hour19,
      Hour20,
      Hour21,
      Hour22,
      Hour23
    )

    def from(num: Int): Option[HourOfDay] = all.find(_.num == num)

    def fromString(num: String): Option[HourOfDay] =
      Try(num.toInt).toOption.flatMap(num => all.find(_.num == num))
  }

  sealed trait DayOfMonth extends Product with Serializable {
    def num: Int
  }

  object DayOfMonth {
    // scalastyle:off
    implicit val ordering: Ordering[DayOfMonth] = Ordering.by(_.num)

    case object Day01 extends DayOfMonth {
      val num = 1
    }

    case object Day02 extends DayOfMonth {
      val num = 2
    }

    case object Day03 extends DayOfMonth {
      val num = 3
    }

    case object Day04 extends DayOfMonth {
      val num = 4
    }

    case object Day05 extends DayOfMonth {
      val num = 5
    }

    case object Day06 extends DayOfMonth {
      val num = 6
    }

    case object Day07 extends DayOfMonth {
      val num = 7
    }

    case object Day08 extends DayOfMonth {
      val num = 8
    }

    case object Day09 extends DayOfMonth {
      val num = 9
    }

    case object Day10 extends DayOfMonth {
      val num = 10
    }

    case object Day11 extends DayOfMonth {
      val num = 11
    }

    case object Day12 extends DayOfMonth {
      val num = 12
    }

    case object Day13 extends DayOfMonth {
      val num = 13
    }

    case object Day14 extends DayOfMonth {
      val num = 14
    }

    case object Day15 extends DayOfMonth {
      val num = 15
    }

    case object Day16 extends DayOfMonth {
      val num = 16
    }

    case object Day17 extends DayOfMonth {
      val num = 17
    }

    case object Day18 extends DayOfMonth {
      val num = 18
    }

    case object Day19 extends DayOfMonth {
      val num = 19
    }

    case object Day20 extends DayOfMonth {
      val num = 20
    }

    case object Day21 extends DayOfMonth {
      val num = 21
    }

    case object Day22 extends DayOfMonth {
      val num = 22
    }

    case object Day23 extends DayOfMonth {
      val num = 23
    }

    case object Day24 extends DayOfMonth {
      val num = 24
    }

    case object Day25 extends DayOfMonth {
      val num = 25
    }

    case object Day26 extends DayOfMonth {
      val num = 26
    }

    case object Day27 extends DayOfMonth {
      val num = 27
    }

    case object Day28 extends DayOfMonth {
      val num = 28
    }

    case object Day29 extends DayOfMonth {
      val num = 29
    }

    case object Day30 extends DayOfMonth {
      val num = 30
    }

    case object Day31 extends DayOfMonth {
      val num = 31
    }

    case object Day32 extends DayOfMonth {
      val num = 32
    }

    val all = Seq(
      Day01,
      Day02,
      Day03,
      Day04,
      Day05,
      Day06,
      Day07,
      Day08,
      Day09,
      Day10,
      Day11,
      Day12,
      Day13,
      Day14,
      Day15,
      Day16,
      Day17,
      Day18,
      Day19,
      Day20,
      Day21,
      Day22,
      Day23,
      Day24,
      Day25,
      Day26,
      Day27,
      Day28,
      Day29,
      Day30,
      Day31,
      Day32
    )

    def from(num: Int): Option[DayOfMonth] = all.find(_.num == num)

    def fromString(num: String): Option[DayOfMonth] =
      Try(num.toInt).toOption.flatMap(num => all.find(_.num == num))
  }

  sealed abstract class Quarter extends Product with Serializable {
    val num: Int
  }

  object Quarter {
    implicit val ordering: Ordering[Quarter] = Ordering.by(_.num)

    case object First extends Quarter {
      val num = 1
    }

    case object Second extends Quarter {
      val num = 2
    }

    case object Third extends Quarter {
      val num = 3
    }

    case object Fourth extends Quarter {
      val num = 4
    }

    val All = Seq(First, Second, Third, Fourth)

    def from(num: Int): Option[Quarter] = All.find(_.num == num)

    def fromString(num: String): Option[Quarter] =
      Try(num.toInt).toOption.flatMap(num => All.find(_.num == num))
  }

  sealed trait Month extends Product with Serializable {
    val name: String
    val abbr: String
    val num: Int
    lazy val quarter: Quarter = Quarter.All
      .find(_.num == ((num - 1) / 3 + 1))
      .getOrElse(throw new RuntimeException("Invalid month"))
  }

  object Month {
    implicit val ordering: Ordering[Month] = Ordering.by(_.num)

    case object January extends Month {
      val name = "January"
      val abbr = "Jan"
      val num = 1
    }

    case object February extends Month {
      val name = "February"
      val abbr = "Feb"
      val num = 2
    }

    case object March extends Month {
      val name = "March"
      val abbr = "Mar"
      val num = 3
    }

    case object April extends Month {
      val name = "April"
      val abbr = "Apr"
      val num = 4
    }

    case object May extends Month {
      val name = "May"
      val abbr = "May"
      val num = 5
    }

    case object June extends Month {
      val name = "June"
      val abbr = "Jun"
      val num = 6
    }

    case object July extends Month {
      val name = "July"
      val abbr = "Jul"
      val num = 7
    }

    case object August extends Month {
      val name = "August"
      val abbr = "Aug"
      val num = 8
    }

    case object September extends Month {
      val name = "September"
      val abbr = "Sep"
      val num = 9
    }

    case object October extends Month {
      val name = "October"
      val abbr = "Oct"
      val num = 10
    }

    case object November extends Month {
      val name = "November"
      val abbr = "Nov"
      val num = 11
    }

    case object December extends Month {
      val name = "December"
      val abbr = "Dec"
      val num = 12
    }

    val all = Seq(
      January,
      February,
      March,
      April,
      May,
      June,
      July,
      August,
      September,
      October,
      November,
      December
    )

    def from(num: Int): Option[Month] = {
      all.find(_.num == num)
    }

    def fromString(str: String): Option[Month] = {
      val intOpt = Try(str.toInt).toOption
      intOpt
        .flatMap(monthNum => all.find(_.num == monthNum))
        .orElse(all.find(_.abbr.toLowerCase == str.toLowerCase))
        .orElse(all.find(_.name.toLowerCase == str.toLowerCase))
    }
  }

  sealed trait WeekOfMonth extends Product with Serializable {
    val num: Int
  }

  object WeekOfMonth {
    implicit val ordering: Ordering[WeekOfMonth] = Ordering.by(_.num)

    case object First extends WeekOfMonth {
      val num = 1
    }

    case object Second extends WeekOfMonth {
      val num = 2
    }

    case object Third extends WeekOfMonth {
      val num = 3
    }

    case object Fourth extends WeekOfMonth {
      val num = 4
    }

    case object Fifth extends WeekOfMonth {
      val num = 5
    }

    val all = Seq(First, Second, Third, Fourth, Fifth)

    def from(num: Int): Option[WeekOfMonth] = all.find(_.num == num)
  }

  case class Year(year: Int) extends AnyVal {
    override def toString: String = year.toString
  }

  object Year {
    implicit val ordering: Ordering[Year] = Ordering.by(_.year)
  }
}

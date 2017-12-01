package com.theseventhsense.datetime

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime.Instant.ParseError
import com.theseventhsense.utils.types.SSDateTime.{Instant, TimeZone}
import moment.{CalendarOpts, Moment}

import scala.scalajs.js

/**
  * Created by erik on 6/15/16.
  */
class MomentRichInstant(instant: Instant)
    extends AbstractRichInstant(instant)
    with MomentImplicits {
  val DefaultHourFormat = "ha"
  val DefaultCalendarOpts = js.Dynamic
    .literal(
      "sameDay" -> s"[Today] $DefaultHourFormat",
      "nextDay" -> s"[Tomorrow] $DefaultHourFormat",
      "nextWeek" -> s"dddd $DefaultHourFormat",
      "lastDay" -> s"[Yesterday] $DefaultHourFormat",
      "lastWeek" -> s"[Last] dddd $DefaultHourFormat",
      "sameElse" -> s"YYYY-MM-DD $DefaultHourFormat"
    )
    .asInstanceOf[CalendarOpts]

  override def asIsoString: String = instant.asMoment.utc().format

  override def asCsvString: String =
    instant.asMoment.utc().format("Y-MM-DD HH:mm:ss.SSS[Z]")

  override def calendarInZone(timeZone: TimeZone): String = {
    val moment = Moment(instant.millis.toDouble)
    moment.calendar(js.undefined, DefaultCalendarOpts)
  }
}

class MomentRichInstantOps extends AbstractRichInstantOps {

  override def fromStringLocalAsUTC(s: String): Either[ParseError, Instant] =
    Either.left(ParseError.Unknown("not implemented"))

  override def fromString(s: String): Either[Instant.ParseError, Instant] =
    SSDateTimeParser
      .parse(s)
      .map(_.instant)
      .leftMap(err => Instant.ParseError.Unknown(err.toString))
}

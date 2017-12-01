package com.theseventhsense.datetime

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime.{Instant, TimeZone}

/**
  * Created by erik on 6/15/16.
  */
abstract class AbstractRichInstant(instant: Instant) extends Serializable {
  def asIsoString: String
  def asCsvString: String
  def calendarInZone(timeZone: TimeZone): String
}

abstract class AbstractRichInstantOps extends Serializable {
  def fromLong(s: String) =
    Either
      .catchNonFatal(s.toLong)
      .map(millis => Instant(millis))
      .leftMap(ex => Instant.ParseError.Unknown(ex.getMessage))

  def fromString(s: String): Either[Instant.ParseError, Instant]

  def fromStringLocalAsUTC(s: String): Either[Instant.ParseError, Instant]

  def parse(s: String): Either[Instant.ParseError, Instant] =
    fromLong(s).orElse(fromString(s))

  def parseLocalAsUTC(s: String): Either[Instant.ParseError, Instant] =
    fromString(s).orElse(fromStringLocalAsUTC(s))
}

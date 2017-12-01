package com.theseventhsense.datetime

import java.time.ZoneId
import java.time.format.{DateTimeFormatter, FormatStyle}

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime.{Instant, TimeZone}

/**
  * Created by erik on 6/15/16.
  */
class JavaTimeRichInstant(instant: Instant)
    extends AbstractRichInstant(instant) {
  def asJavaTime: java.time.Instant =
    java.time.Instant.ofEpochMilli(instant.millis)

  override def asIsoString: String =
    DateTimeFormatter.ISO_INSTANT.format(asJavaTime)

  val UTC = ZoneId.of("UTC")

  override def asCsvString: String =
    SSDateTimeParser.csvDateTimeFormatter.format(asJavaTime.atZone(UTC))

  override def calendarInZone(timeZone: TimeZone): String = {
    val javaInstant = java.time.Instant.ofEpochMilli(instant.millis)
    val javaZone = java.time.ZoneId.of(timeZone.name)
    val javaZonedTime =
      java.time.ZonedDateTime.ofInstant(javaInstant, javaZone)
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    javaZonedTime.format(formatter)
  }
}

class JavaTimeRichInstantOps
    extends AbstractRichInstantOps
    with JavaTimeImplicits {
  val UTC = ZoneId.of("UTC")
  override def fromStringLocalAsUTC(
    s: String
  ): Either[Instant.ParseError, Instant] =
    Either
      .catchNonFatal(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(s))
      .map(java.time.LocalDateTime.from)
      .map(_.atZone(UTC).asU.instant)
      .leftMap { case err => Instant.ParseError.Unknown(err.toString) }

  /**
    * Parse instanct strings, replacing " " with "T" such that "2017-01-01 00:00:00Z" becomes "2017-01-01T00:00:00Z"
    * @param s
    * @return
    */
  def parseInstant(s: String): Either[Instant.ParseError, Instant] =
    Either
      .catchNonFatal(java.time.Instant.parse(s.replace(" ", "T")))
      .map(_.asU)
      .leftMap {
        case err => Instant.ParseError.Unknown(err.toString)
      }

  def parseZoned(s: String): Either[Instant.ParseError, Instant] =
    SSDateTimeParser.parse(s).map { case dt => dt.instant }.leftMap {
      case err => Instant.ParseError.Unknown(err.toString)
    }

  override def fromString(s: String): Either[Instant.ParseError, Instant] =
    parseInstant(s).orElse(parseZoned(s))
}

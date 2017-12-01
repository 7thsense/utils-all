package com.theseventhsense.datetime

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.{ZoneId, ZonedDateTime}

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime.DateTime

object SSDateTimeParser extends TSSDateTimeParser with JavaTimeImplicits {
  // Force the default timezone to be UTC

  lazy val Eastern = ZoneId.of("US/Eastern")
  lazy val Central = ZoneId.of("US/Central")
  lazy val Mountain = ZoneId.of("US/Mountain")
  lazy val Pacific = ZoneId.of("US/Pacific")

  lazy val noDateSeperatorsDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMddHH:mm:ss ZZZ")

  lazy val noOffsetSeperatorDateTimeFormatter1: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")

  lazy val noOffsetSeperatorDateTimeFormatter2: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX")

  lazy val noOffsetSeperatorDateTimeFormatter3: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

  lazy val dateTimeFormatterWithTimeZone: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss zzz")

  lazy val spacesDateTimeFormatterWithTimeZone: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzzz")

  lazy val csvDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSX")

  lazy val marketoProgramDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXZZZZZ")

  lazy val formatters: Seq[DateTimeFormatter] = Seq(
    csvDateTimeFormatter,
    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
    DateTimeFormatter.ISO_INSTANT,
    DateTimeFormatter.RFC_1123_DATE_TIME,
    noOffsetSeperatorDateTimeFormatter1,
    noOffsetSeperatorDateTimeFormatter2,
    noOffsetSeperatorDateTimeFormatter3,
    noDateSeperatorsDateTimeFormatter,
    dateTimeFormatterWithTimeZone,
    spacesDateTimeFormatterWithTimeZone,
    DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss ZZZZZ"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZZZZ")
  )

  lazy val flexibleFormatter: DateTimeFormatter = {
    val builder = new DateTimeFormatterBuilder()
    formatters.foreach(builder.appendOptional)
    builder.toFormatter
  }

  val timeZoneAbbreviations: Map[String, ZoneId] = Map(
    "EST" -> Eastern,
    "EDT" -> Eastern,
    "CST" -> Central,
    "CDT" -> Central,
    "MST" -> Mountain,
    "MDT" -> Mountain,
    "PST" -> Pacific,
    "PDT" -> Pacific
  )

  def parseDateTime(
    dateTimeString: String
  ): Either[DateTime.ParseError, ZonedDateTime] = {
    if (Option(dateTimeString).isEmpty || dateTimeString == "" ||
        dateTimeString == "null") {
      Either.right(
        ZonedDateTime
          .ofInstant(java.time.Instant.ofEpochMilli(0L), ZoneId.of("UTC"))
      )
    } else if (isAllDigits(dateTimeString)) {
      Either.right(fromLong(dateTimeString.toLong))
    } else {
      var dts = dateTimeString
      for ((abbr, zone) <- timeZoneAbbreviations.toSeq) {
        dts = dts.replace(abbr, zone.getId)
      }
      dts = dts.replace("Z+0000", "Z")
      Either
        .catchNonFatal(
          ZonedDateTime.parse(dts, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        )
        .orElse(
          Either.catchNonFatal(ZonedDateTime.parse(dts, flexibleFormatter))
        )
        .leftMap(ex => DateTime.ParseError.Unknown(ex.getMessage))
    }
  }

  def fromLong(number: Long): ZonedDateTime = {
    ZonedDateTime.ofInstant(
      java.time.Instant.ofEpochMilli(number),
      ZoneId.of("UTC")
    )
  }

  def isAllDigits(x: String): Boolean = x forall Character.isDigit

  override def parse(
    dateTimeString: String
  ): Either[DateTime.ParseError, DateTime] =
    parseDateTime(dateTimeString).map(_.asU)
}

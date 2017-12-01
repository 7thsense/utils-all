package com.theseventhsense.datetime

import java.time.ZonedDateTime

import com.theseventhsense.utils.types.SSDateTime.{DateTime, Instant, TimeZone}

/**
  * Created by erik on 6/15/16.
  */
trait JavaTimeImplicits {

  implicit class RichJavaTimeInstant(instant: java.time.Instant) {
    def asU: Instant = Instant(instant.toEpochMilli)
  }

  implicit class RichJavaTimeZoneId(zoneId: java.time.ZoneId) {
    def asU: TimeZone = TimeZone.from(zoneId.getId)
  }

  implicit class RichJavaTimeZonedDateTime(zonedDateTime: ZonedDateTime) {
    def asU: DateTime =
      DateTime.apply(
        zonedDateTime.toOffsetDateTime.toInstant.asU,
        zonedDateTime.getZone.asU
      )
  }
}

object JavaTimeImplicits extends JavaTimeImplicits

package com.theseventhsense.datetime

import java.time.ZoneId

import com.theseventhsense.utils.types.SSDateTime.TimeZone

/**
  * Created by erik on 6/15/16.
  */
trait JavaTimeTimeZoneImplicits {
  implicit class ImplicitJavaTimeRichTimeZone(timeZone: TimeZone)
      extends JavaTimeRichTimeZone(timeZone) {
    def asJavaTime: java.time.ZoneId = ZoneId.of(timeZone.name)
  }
}

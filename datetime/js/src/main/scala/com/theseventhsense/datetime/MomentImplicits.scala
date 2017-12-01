package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime.{DateTime, Instant, TimeZone}
import moment.{Date, Moment}

/**
  * Created by erik on 6/15/16.
  */
trait MomentImplicits {
  implicit class RichMomentDateTime(dateTime: DateTime) {
    def asMoment: Date = dateTime.instant.asMoment
  }
  implicit class RichMomentInstant(instant: Instant) {
    def asMoment: Date = Moment(instant.millis.toDouble)
  }
  implicit class RichMomentTimeZone(timeZone: TimeZone) {
    def asMoment: String = timeZone.name
  }
}

object MomentImplicits extends MomentImplicits

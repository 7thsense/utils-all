package com.theseventhsense.datetime

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime.{DateTime, Instant, TimeZone}
import moment.Moment

/**
  * Created by erik on 12/26/15.
  */
object SSDateTimeParser extends TSSDateTimeParser {
  override def parse(
    dateTimeString: String
  ): Either[DateTime.ParseError, DateTime] = {
    val date = Moment(dateTimeString).utc().toDate()
    val millis = date.getTime()
    val longMillis = millis.toLong
    val instant = Instant(longMillis)
    Either.right(DateTime(instant, TimeZone.UTC))
  }
}

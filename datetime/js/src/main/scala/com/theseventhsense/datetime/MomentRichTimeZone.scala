package com.theseventhsense.datetime

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime.{Instant, TimeZone}

/**
  * Created by erik on 6/15/16.
  */
class MomentRichTimeZone(timeZone: TimeZone)
    extends AbstractRichTimeZone(timeZone) {
  override def valid: Boolean = false

  override def offsetSecondsAt(instant: Instant): Integer = 0
}

class MomentRichTimezoneOps extends AbstractRichTimeZoneOps {
  override def parse(s: String): Either[TimeZone.ParseError, TimeZone] =
    Either.left(TimeZone.ParseError.NotImplemented)
}

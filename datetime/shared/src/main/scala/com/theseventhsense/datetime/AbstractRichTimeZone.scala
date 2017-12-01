package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime
import com.theseventhsense.utils.types.SSDateTime.TimeZone

/**
  * Created by erik on 6/15/16.
  */
abstract class AbstractRichTimeZone(timeZone: TimeZone) extends Serializable {
  def valid: Boolean

  def offsetSecondsAt(instant: SSDateTime.Instant = SSDateTime.now): Integer
}

abstract class AbstractRichTimeZoneOps extends Serializable {
  def parse(s: String): Either[TimeZone.ParseError, SSDateTime.TimeZone]
}

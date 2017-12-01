package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime

import scala.util.Try

/**
  * Created by erik on 2/18/16.
  */
trait TSSDateTimeZoneOps extends Serializable {
  def isValid(s: String): Boolean
  def offsetSeconds(
      zone: SSDateTime.TimeZone, instant: SSDateTime.Instant): Integer
  def parse(s: String): Option[SSDateTime.TimeZone]
  def instantAsIsoString(instant: SSDateTime.Instant): String

  def normalizeOffset(offset: String): Option[String] = {
    Try(Integer.parseInt(offset)).toOption.map(o => "%+05d".format(o))
  }
}

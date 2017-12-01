package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime.DateTime

/**
  * Created by erik on 12/26/15.
  */
trait TSSDateTimeParser extends Serializable {
  def parse(dateTimeString: String): Either[DateTime.ParseError, DateTime]
}

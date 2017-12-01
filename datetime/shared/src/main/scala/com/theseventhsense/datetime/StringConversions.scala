package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime.{DayOfWeek, HourOfDay}
import scala.language.implicitConversions

/**
  * Created by erik on 6/22/16.
  */
object StringConversions extends Serializable {
  implicit def convertStringToDayOfWeek(s: String): DayOfWeek =
    DayOfWeek.fromString(s).get

  implicit def convertStringToHourOfDay(s: String): HourOfDay =
    HourOfDay.fromString(s).get
}

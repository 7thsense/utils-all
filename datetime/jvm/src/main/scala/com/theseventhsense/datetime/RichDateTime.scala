package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime.DateTime

/**
  * Created by erik on 6/15/16.
  */
class RichDateTime(dateTime: DateTime) extends JavaTimeRichDateTime(dateTime)

object RichDateTime extends JavaTimeRichDateTimeOps

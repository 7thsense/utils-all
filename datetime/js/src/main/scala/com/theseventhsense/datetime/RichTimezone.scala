package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime.TimeZone

/**
  * Created by erik on 6/15/16.
  */
class RichTimeZone(timeZone: TimeZone) extends MomentRichTimeZone(timeZone)

object RichTimeZone extends MomentRichTimezoneOps

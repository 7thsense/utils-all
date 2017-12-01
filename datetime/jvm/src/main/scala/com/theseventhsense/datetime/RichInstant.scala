package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime.Instant

/**
  * Created by erik on 6/15/16.
  */
class RichInstant(instant: Instant) extends JavaTimeRichInstant(instant)

object RichInstant extends JavaTimeRichInstantOps

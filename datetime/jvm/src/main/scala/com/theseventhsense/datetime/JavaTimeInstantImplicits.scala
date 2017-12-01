package com.theseventhsense.datetime

import com.theseventhsense.utils.types.SSDateTime.Instant

/**
  * Created by erik on 6/15/16.
  */
trait JavaTimeInstantImplicits {
  implicit class ImplicitJavaTimeRichInstant(instant: Instant)
      extends JavaTimeRichInstant(instant)
}

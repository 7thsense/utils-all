package com.theseventhsense.clients.wsclient

import com.theseventhsense.utils.persistence.Keyed
import com.theseventhsense.utils.types.SSDateTime

/**
  * Created by erik on 9/26/16.
  */
trait KeyedTimestamp extends Keyed {
  def timestamp: SSDateTime.Instant = SSDateTime.now
}

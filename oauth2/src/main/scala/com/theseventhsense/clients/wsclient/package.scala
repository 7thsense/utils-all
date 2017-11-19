package com.theseventhsense.clients

import com.theseventhsense.utils.retry.RetryStrategy
import play.api.libs.ws.WSResponse

package object wsclient {
  type RestClientRetryStrategy = RetryStrategy[WSResponse]
}

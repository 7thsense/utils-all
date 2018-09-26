package com.theseventhsense.clients

import play.api.libs.ws.StandaloneWSResponse

import com.theseventhsense.utils.retry.RetryStrategy

package object wsclient {
  type RestClientRetryStrategy = RetryStrategy[StandaloneWSResponse]
}

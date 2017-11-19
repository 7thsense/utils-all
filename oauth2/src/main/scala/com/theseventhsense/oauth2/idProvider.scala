package com.theseventhsense.oauth2

import play.api.mvc.{RequestHeader, Request}

import scala.util.Try

/**
  * In order to properly track the state of an OAuth flow, we need to be able
  * to tie it back to a unique user. The RequestIdProvider is how we do that.
  */
trait IdProvider {
  def uid(implicit request: RequestHeader): Try[String]
}

/**
  * The RemoteAddressIdProvider is a dummy request ID provider to make it easy
  * to test the oauth flows in isolation. It simply uses the browsers IP address
  * as the user id. It should never be used in production.
  */
class RemoteAddressIdProvider extends IdProvider {
  def uid(implicit request: RequestHeader): Try[String] = Try {
    request.remoteAddress
  }
}

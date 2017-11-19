package com.theseventhsense

/**
  * Created by erik on 6/21/16.
  */
package object oauth2 {
  final val CALLBACK_URL_PATH_KEY = "OAUTH2_CALLBACK_URL"
  final val COOKIE_NAME_KEY = "OAUTH2_COOKIE_NAME"
  final val DEFAULT_STATE_COOKIE_NAME = "oauth2-state"
  final val DEFAULT_CALLBACK_URL_PATH = "/oauth2/client/:providerName/callback"
}

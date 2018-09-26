package com.theseventhsense.oauth2

import play.api.libs.ws.StandaloneWSResponse

/**
  * Created by erik on 4/7/16.
  */
object OAuth2ResponseHandler {
  class Http401 extends OAuth2ResponseHandler {
    override def shouldRefresh(response: StandaloneWSResponse): Boolean = {
      response.status == 401
    }
  }

  def Default = new Http401
}

abstract class OAuth2ResponseHandler {
  def shouldRefresh(response: StandaloneWSResponse): Boolean
}

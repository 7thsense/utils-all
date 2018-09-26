package com.theseventhsense.testing

import akka.stream.Materializer
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.ahc.StandaloneAhcWSClient

trait WSClientFactory {
  implicit def materializer: Materializer

  implicit def wsClient: StandaloneWSClient = StandaloneAhcWSClient()
}

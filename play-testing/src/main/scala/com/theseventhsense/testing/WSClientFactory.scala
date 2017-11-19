package com.theseventhsense.testing

import akka.stream.Materializer
import play.api.libs.ws.ahc.AhcWSClient

trait WSClientFactory {
  implicit def materializer: Materializer

  implicit def wsClient: AhcWSClient = AhcWSClient()
}

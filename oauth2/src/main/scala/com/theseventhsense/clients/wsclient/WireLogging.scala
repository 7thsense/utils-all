package com.theseventhsense.clients.wsclient
import scala.concurrent.ExecutionContext

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import play.api.libs.ws.StandaloneWSRequest

object WireLogging {
  private[wsclient] val wireLogger: LoggerTakingImplicit[LogContext] = Logger
    .takingImplicit[LogContext](this.getClass.getPackage.getName + ".wire")

  implicit class WireLoggingWSRequest(request: StandaloneWSRequest) {
    def withOptionalWireLogging()(
      implicit ec: ExecutionContext,
      logContext: LogContext
    ): StandaloneWSRequest =
      if (logContext.shouldWireLog(request))
        request.withRequestFilter(new WSClientCurlRequestFilter(wireLogger))
      else request
  }
}

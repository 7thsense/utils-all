package com.theseventhsense.clients.wsclient
import com.typesafe.scalalogging.LoggerTakingImplicit
import play.api.libs.ws.ahc.{CurlFormat, StandaloneAhcWSRequest}
import play.api.libs.ws.{WSRequestExecutor, WSRequestFilter}

class WSClientCurlRequestLogger(logger: LoggerTakingImplicit[LogContext])(
  implicit logContext: LogContext
) extends WSRequestFilter
    with CurlFormat {
  def apply(executor: WSRequestExecutor): WSRequestExecutor = {
    WSRequestExecutor { request =>
      logger.trace(toCurl(request.asInstanceOf[StandaloneAhcWSRequest]))
      executor(request)
    }
  }
}

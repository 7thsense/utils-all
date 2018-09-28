package com.theseventhsense.clients.wsclient
import scala.concurrent.ExecutionContext

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import play.api.libs.ws.ahc.{CurlFormat, StandaloneAhcWSRequest}
import play.api.libs.ws.{StandaloneWSRequest, StandaloneWSResponse, WSRequestExecutor, WSRequestFilter}

import com.theseventhsense.utils.logging.LogContext

object WireLogging {
  private[wsclient] val wireLogger: LoggerTakingImplicit[LogContext] = Logger
    .takingImplicit[LogContext](this.getClass.getPackage.getName + ".wire")

  private class WSClientCurlRequestFilter(logger: LoggerTakingImplicit[LogContext])(
    implicit
    ec: ExecutionContext,
    logContext: LogContext
  ) extends WSRequestFilter
    with CurlFormat {
    def asHttp(request: StandaloneWSRequest,
               response: StandaloneWSResponse): String = {
      toCurl(request.asInstanceOf[StandaloneAhcWSRequest]) + "\n" +
        s"HTTP/1.1 ${response.status} ${response.statusText}\n" +
        response.headers
          .flatMap {
            case (header, values) =>
              values.map(value => s"$header: $value")
          }
          .mkString("\n") + "\n" + response.body.linesWithSeparators.take(20).mkString("\n")
    }

    def apply(executor: WSRequestExecutor): WSRequestExecutor = {
      WSRequestExecutor { request =>
        executor(request).map { response =>
          logger.trace(asHttp(request, response))
          response
        }
      }
    }
  }

  implicit class WireLoggingWSRequest(request: StandaloneWSRequest) {
    def withOptionalWireLogging()(
      implicit ec: ExecutionContext,
      logContext: LogContext
    ): StandaloneWSRequest =
      if (logContext.shouldLog(request))
        request.withRequestFilter(new WSClientCurlRequestFilter(wireLogger))
      else request
  }
}

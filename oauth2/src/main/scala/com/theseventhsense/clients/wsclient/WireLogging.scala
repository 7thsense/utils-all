package com.theseventhsense.clients.wsclient
import scala.concurrent.ExecutionContext

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import play.api.libs.ws.ahc.{CurlFormat, StandaloneAhcWSRequest}
import play.api.libs.ws.{
  StandaloneWSRequest,
  StandaloneWSResponse,
  WSRequestExecutor,
  WSRequestFilter
}

import com.theseventhsense.utils.logging.LogContext
import com.theseventhsense.utils.models.TLogContext

object WireLogging {
  import LogContext._
  private[wsclient] val wireLogger: LoggerTakingImplicit[TLogContext] = Logger
    .takingImplicit[TLogContext](this.getClass.getPackage.getName + ".wire")

  private class WSClientCurlRequestFilter(
    logger: LoggerTakingImplicit[TLogContext]
  )(implicit
    ec: ExecutionContext,
    logContext: TLogContext)
      extends WSRequestFilter
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
          .mkString("\n") + "\n" + response.body.linesWithSeparators
        .take(20)
        .mkString("\n")
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
    def withOptionalWireLogging()(implicit ec: ExecutionContext,
                                  logContext: TLogContext): StandaloneWSRequest =
      if (logContext.shouldLog(request))
        request.withRequestFilter(new WSClientCurlRequestFilter(wireLogger))
      else request
  }
}

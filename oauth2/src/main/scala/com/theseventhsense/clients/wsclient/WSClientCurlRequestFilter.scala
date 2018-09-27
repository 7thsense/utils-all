package com.theseventhsense.clients.wsclient
import scala.concurrent.ExecutionContext

import com.typesafe.scalalogging.LoggerTakingImplicit
import play.api.libs.ws.ahc.{CurlFormat, StandaloneAhcWSRequest}
import play.api.libs.ws.{
  StandaloneWSRequest,
  StandaloneWSResponse,
  WSRequestExecutor,
  WSRequestFilter
}

class WSClientCurlRequestFilter(logger: LoggerTakingImplicit[LogContext])(
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
        .mkString("\n") + "\n" + response.body.linesWithSeparators.take(20)
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

package com.theseventhsense.clients.wsclient
import java.nio.charset.StandardCharsets
import java.util.Base64

import scala.concurrent.ExecutionContext

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import play.api.libs.ws.ahc.{CurlFormat, StandaloneAhcWSRequest}
import play.api.libs.ws._
import play.shaded.ahc.org.asynchttpclient.util.HttpUtils

import com.theseventhsense.utils.logging.LogContext
import com.theseventhsense.utils.models.TLogContext

object WireLogging {
  import LogContext._
  private[wsclient] val wireLogger: LoggerTakingImplicit[TLogContext] = Logger
    .takingImplicit[TLogContext](this.getClass.getPackage.getName + ".wire")

  private def toCurl(request: StandaloneAhcWSRequest): String = {
    val b = new StringBuilder("curl \\\n")

    // verbose, since it's a fair bet this is for debugging
    b.append("  --verbose")
    b.append(" \\\n")

    // method
    b.append(s"  --request ${request.method}")
    b.append(" \\\n")

    //authentication
    request.auth match {
      case Some((userName, password, WSAuthScheme.BASIC)) =>
        val encodedPassword = Base64.getUrlEncoder.encodeToString(s"$userName:$password".getBytes(StandardCharsets.US_ASCII))
        b.append(s"""  --header 'Authorization: Basic ${quote(encodedPassword)}'""")
        b.append(" \\\n")
      case _ => Unit
    }

    // headers
    request.headers.foreach {
      case (k, values) =>
        values.foreach { v =>
          b.append(s"  --header '${quote(k)}: ${quote(v)}'")
          b.append(" \\\n")
        }
    }

    // cookies
    request.cookies.foreach { cookie =>
      b.append(s"""  --cookie '${cookie.name}=${cookie.value}'""")
      b.append(" \\\n")
    }

    // body (note that this has only been checked for text, not binary)
    request.body match {
      case EmptyBody => // Do nothing.
      case InMemoryBody(byteString) =>
        val charset = findCharset(request)
        val bodyString = byteString.decodeString(charset)
        // XXX Need to escape any quotes within the body of the string.
        b.append(s"  --data '${quote(bodyString)}'")
        b.append(" \\\n")
      case SourceBody(_) =>
        b.append(s" --data 'stream'")
        b.append(" \\\n")
      case other =>
        b.append(s" --data 'Unsupported body type " + other.getClass + "'")
        b.append(" \\\n")
    }

    // pull out some underlying values from the request.  This creates a new Request
    // but should be harmless.
    val asyncHttpRequest = request.buildRequest()
    val proxyServer = asyncHttpRequest.getProxyServer
    if (proxyServer != null) {
      b.append(s"  --proxy ${proxyServer.getHost}:${proxyServer.getPort}")
      b.append(" \\\n")
    }

    // url
    b.append(s"  '${quote(asyncHttpRequest.getUrl)}'")

    val curlOptions = b.toString()
    curlOptions
  }

  protected def findCharset(request: StandaloneAhcWSRequest): String = {
    request.contentType.map { ct =>
      Option(HttpUtils.extractContentTypeCharsetAttribute(ct)).getOrElse {
        StandardCharsets.UTF_8
      }.name()
    }.getOrElse(HttpUtils.extractContentTypeCharsetAttribute("UTF-8").name())
  }

  def quote(unsafe: String): String = unsafe.replace("'", "'\\''")

  private class WSClientCurlRequestFilter(
    logger: LoggerTakingImplicit[TLogContext]
  )(implicit
    ec: ExecutionContext,
    logContext: TLogContext)
      extends WSRequestFilter {
    def asHttp(request: StandaloneWSRequest,
               response: StandaloneWSResponse): String = {
      toCurl(request.asInstanceOf[StandaloneAhcWSRequest]) + "\n\n" +
        s"HTTP/1.1 ${response.status} ${response.statusText}\n" +
        response.headers
          .flatMap {
            case (header, values) =>
              values.map(value => s"$header: $value")
          }
          .mkString("\n") + "\n\n" + response.body.linesWithSeparators
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

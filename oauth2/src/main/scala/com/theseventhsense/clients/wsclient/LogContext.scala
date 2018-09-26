package com.theseventhsense.clients.wsclient
import com.typesafe.scalalogging.CanLog
import org.slf4j.MDC
import play.api.libs.ws.StandaloneWSRequest

abstract class LogContext extends Product with Serializable {
  def context: Map[String, String]
  def shouldWireLog: StandaloneWSRequest => Boolean

  def keys: Iterable[String] = context.keys
}

object LogContext {
  implicit val logContextCanLog: CanLog[LogContext] = new CanLog[LogContext] {
    override def logMessage(originalMsg: String, a: LogContext): String = {
      a.context.foreach {
        case (k, v) =>
          MDC.put(k, v)
      }
      originalMsg
    }
    override def afterLog(a: LogContext): Unit = {
      a.keys.foreach(MDC.remove)
      super.afterLog(a)
    }
  }
}

case class WSClientLogContext(context: Map[String, String] = Map.empty,
                              shouldWireLog: StandaloneWSRequest => Boolean =
                                _ => false)
    extends LogContext

package com.theseventhsense.utils.logging

import com.typesafe.scalalogging.CanLog
import org.slf4j.MDC

abstract class LogContext extends Serializable {
  def context: Map[String, String]
  def shouldLog: Any => Boolean

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

  val empty: LogContext = new LogContext {
    override val context: Map[String, String] = Map.empty
    override val shouldLog: Any => Boolean = _ => false
  }
}

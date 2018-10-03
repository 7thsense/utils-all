package com.theseventhsense.utils.logging

import com.theseventhsense.utils.models.TLogContext
import com.typesafe.scalalogging.CanLog
import org.slf4j.MDC

abstract class LogContext extends TLogContext

object LogContext {
  implicit val logContextCanLog: CanLog[LogContext] =  new CanLog[LogContext] {
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

  implicit val tLogContextCanLog: CanLog[TLogContext] = new CanLog[TLogContext] {
    override def logMessage(originalMsg: String, a: TLogContext): String = {
      a.context.foreach {
        case (k, v) =>
          MDC.put(k, v)
      }
      originalMsg
    }
    override def afterLog(a: TLogContext): Unit = {
      a.keys.foreach(MDC.remove)
      super.afterLog(a)
    }
  }

  val empty: LogContext = new LogContext {
    override val context: Map[String, String] = Map.empty
    override val shouldLog: Any => Boolean = _ => false
  }
}

package com.theseventhsense.utils.logging

import com.theseventhsense.utils.models.TLogContext
import com.typesafe.scalalogging.CanLog
import org.slf4j.MDC

abstract class LogContext extends TLogContext

object LogContext {
  implicit val logContextCanLog: CanLog[TLogContext] = new CanLog[TLogContext] {
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

  val empty: TLogContext = new TLogContext {
    override val context: Map[String, String] = Map.empty
    override val shouldLog: Any => Boolean = _ => false
  }
}

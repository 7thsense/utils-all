package com.theseventhsense.clients.wsclient
import play.api.libs.json.Json

import com.theseventhsense.utils.logging.LogContext
import com.theseventhsense.utils.types.SSDateTime
import com.theseventhsense.udatetime.UDateTimeFormats._

object LastModifiedStateHandler {
  val LastModifiedDate = "last-modified"
}

class LastModifiedStateHandler[T <: KeyedTimestamp] extends StateHandler[T] {
  import LastModifiedStateHandler._

  override def filter(obj: T)(implicit lc: LogContext): Boolean = {
    val lastModifiedOpt =
      get(obj.key, LastModifiedDate).flatMap(_.asOpt[SSDateTime.Instant])
    val result = lastModifiedOpt match {
      case None    => true
      case Some(x) => x.isAfter(SSDateTime.now.minusDays(30))
    }
    if (!result) {
      logger.trace(
        s"${obj.key} has been loaded and is older than 30 days, skipping"
      )
    }
    result
  }

  /**
    * Compute ordering based on last modified date
    * - unseen keys first, alphabetical by key
    * - seen keys sorted by descending date
    * @return
    */
  override def ordering(implicit lc: LogContext): Ordering[T] = new Ordering[T] {
    override def compare(x: T, y: T): Int = {
      val X = get(x.key, LastModifiedDate).flatMap(_.asOpt[SSDateTime.Instant])
      val Y = get(y.key, LastModifiedDate).flatMap(_.asOpt[SSDateTime.Instant])
      if (X.isDefined && Y.isDefined) {
        X.get.compareTo(Y.get)
      } else {
        if (X.isDefined) {
          logger.trace(s"${x.key} is seen, ${y.key} not seen, ${y.key} first")
          1
        } else if (Y.isDefined) {
          logger.trace(s"${y.key} is seen, ${x.key} not seen, ${x.key} first")
          -1
        } else {
          x.key.compareTo(y.key)
        }
      }
    }
  }

  /**
    * Store the last modification date for obj
    * @param obj
    */
  override def update(obj: T)(implicit lc: LogContext): T = {
    update(obj.key, obj.timestamp)
    obj
  }

  /**
    * Store the last modification date for obj
    * @param key
    * @param timestamp
    */
  override def update(key: String, timestamp: SSDateTime.Instant)(implicit lc: LogContext): Unit = {
    def setTimestamp(): Unit = {
      set(key, LastModifiedDate, Json.toJson(timestamp))
    }
    super.update(key, timestamp)
    get(key, LastModifiedDate).flatMap(_.asOpt[SSDateTime.Instant]) match {
      case None => setTimestamp()
      case Some(d: SSDateTime.Instant) if d.isBefore(timestamp) =>
        setTimestamp()
      case _ =>
    }
  }

}
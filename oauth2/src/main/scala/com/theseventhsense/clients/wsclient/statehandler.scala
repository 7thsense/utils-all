package com.theseventhsense.clients.wsclient

import com.theseventhsense.utils.logging.Logging
import com.theseventhsense.utils.types.SSDateTime
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import com.theseventhsense.udatetime.UDateTimeFormats._

object StateHandler {
  val CountKey = "count"
}

trait StateHandler[T <: KeyedTimestamp] extends Logging {

  import StateHandler._

  var lastKey: String = ""
  var data: JsObject = Json.obj()

  def update(obj: T): T

  def update(key: String, timestamp: SSDateTime.Instant): Unit = {
    val count: Long =
      get(key, CountKey).getOrElse(JsNumber(0L)).asOpt[Long].getOrElse(0L)
    set(key, CountKey, JsNumber(count + 1))
    if (key != lastKey || count % 1000 == 0) {
      val totalCount = data.values.foldLeft(0L) {
        case (acc, item) =>
          val count: Long = item.asOpt[JsObject] match {
            case None => 0
            case Some(obj: JsObject) =>
              (obj \ CountKey).asOpt[Long].getOrElse(0L)
          }
          acc + count
      }
      lastKey = key
      logger.trace(s"Updating state $key -> $timestamp, $count / $totalCount")
    }
  }

  def get(key: String, subKey: String): Option[JsValue] = {
    (data \ key).toOption.flatMap(obj => (obj \ subKey).toOption)
  }

  def set(key: String, subKey: String, value: JsValue): Unit = {
    val keyObj: JsObject = (data \ key).asOpt[JsObject].getOrElse(Json.obj())
    val newKeyObj: JsObject = keyObj ++ Json.obj(subKey -> value)
    data += key -> newKeyObj
  }

  def filter(obj: T): Boolean

  def ordering: Ordering[T]

  def fromJson(json: JsValue): Boolean = {
    json
      .validate[JsObject]
      .map { data =>
        this.data = data
        logger.trace(s"Loaded data: $data")
        true
      }
      .recoverTotal { e =>
        logger.warn("Failed to load state", e)
        false
      }
  }

  def toJson: JsValue = {
    Json.toJson(data)
  }
}

object LastModifiedStateHandler {
  val LastModifiedDate = "last-modified"
}

class LastModifiedStateHandler[T <: KeyedTimestamp] extends StateHandler[T] {

  import LastModifiedStateHandler._

  override def filter(obj: T): Boolean = {
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
  override def ordering: Ordering[T] = new Ordering[T] {
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
  override def update(obj: T): T = {
    update(obj.key, obj.timestamp)
    obj
  }

  /**
    * Store the last modification date for obj
    * @param key
    * @param timestamp
    */
  override def update(key: String, timestamp: SSDateTime.Instant): Unit = {
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

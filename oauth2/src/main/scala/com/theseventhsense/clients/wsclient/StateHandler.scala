package com.theseventhsense.clients.wsclient

import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}

import com.theseventhsense.utils.logging.{LogContext, Logging}
import com.theseventhsense.utils.types.SSDateTime

object StateHandler {
  val CountKey = "count"
}

trait StateHandler[T <: KeyedTimestamp] extends Logging {

  import StateHandler._

  var lastKey: String = ""
  var data: JsObject = Json.obj()

  def update(key: String,
             timestamp: SSDateTime.Instant)(implicit lc: LogContext): Unit = {
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

  def get(key: String, subKey: String): Option[JsValue] =
    (data \ key).toOption.flatMap(obj => (obj \ subKey).toOption)

  def set(key: String, subKey: String, value: JsValue): Unit = {
    val keyObj: JsObject = (data \ key).asOpt[JsObject].getOrElse(Json.obj())
    val newKeyObj: JsObject = keyObj ++ Json.obj(subKey -> value)
    data += key -> newKeyObj
  }


  def fromJson(json: JsValue)(implicit lc: LogContext): Boolean =
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


  def toJson: JsValue =
    Json.toJson(data)

  def ordering(implicit lc: LogContext): Ordering[T]

  def update(obj: T)(implicit lc: LogContext): T

  def filter(obj: T)(implicit lc: LogContext): Boolean

}





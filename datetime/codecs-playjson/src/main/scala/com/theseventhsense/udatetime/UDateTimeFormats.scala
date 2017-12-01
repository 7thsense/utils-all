package com.theseventhsense.udatetime

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime
import play.api.libs.json._

/**
  * Created by erik on 6/21/16.
  */
trait UDateTimeFormats {
  implicit val uDateTimeZoneReads: Reads[SSDateTime.TimeZone] =
    new Reads[SSDateTime.TimeZone] {
      def reads(json: JsValue): JsResult[SSDateTime.TimeZone] = json match {
        case JsString(s) => JsSuccess(SSDateTime.TimeZone.from(s))
        case _ =>
          JsError(
            Seq(
              JsPath() ->
                Seq(JsonValidationError("validate.error.expected.datetimezone.id"))
            )
          )
      }
    }

  implicit lazy val writes: Writes[SSDateTime.TimeZone] =
    new Writes[SSDateTime.TimeZone] {
      def writes(z: SSDateTime.TimeZone): JsValue = JsString(z.name)
    }

  implicit val uDateTimeInstantReads: Reads[SSDateTime.Instant] =
    new Reads[SSDateTime.Instant] {
      def reads(json: JsValue): JsResult[SSDateTime.Instant] = json match {
        case JsNumber(d) => JsSuccess(SSDateTime.Instant(d.toLong))
        case JsString(s) =>
          SSDateTime.Instant.parse(s) match {
            case Right(d) => JsSuccess(d)
            case Left(e) =>
              JsError(
                Seq(
                  JsPath() ->
                    Seq(JsonValidationError("validate.error.unexpected.format", e))
                )
              )
          }
        case _ =>
          JsError(
            Seq(
              JsPath() -> Seq(JsonValidationError("validate.error.expected.date"))
            )
          )
      }
    }

  implicit val uDateTimeInstantWrites: Writes[SSDateTime.Instant] =
    new Writes[SSDateTime.Instant] {
      def writes(d: SSDateTime.Instant): JsValue = JsNumber(d.millis)
    }

  implicit val uDateTimeInstantFormat: Format[SSDateTime.Instant] =
    Format[SSDateTime.Instant](uDateTimeInstantReads, uDateTimeInstantWrites)
}

object UDateTimeFormats extends UDateTimeFormats

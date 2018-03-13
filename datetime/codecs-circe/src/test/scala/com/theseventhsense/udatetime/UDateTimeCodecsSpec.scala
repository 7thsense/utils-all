package com.theseventhsense.udatetime

import cats.implicits._
import io.circe.generic.semiauto._
import io.circe._
import org.scalatest._

import com.theseventhsense.utils.types.SSDateTime

class UDateTimeCodecsSpec extends WordSpec with MustMatchers{
  case class TestObj(cTime: SSDateTime.Instant)
  object TestObj {
    implicit val instantDecoder= UDateTimeCodecs.instantDecoder
    implicit val instantEncoder = UDateTimeCodecs.instantEncoder
    implicit val encoder: Encoder[TestObj] = deriveEncoder
    implicit val decoder: Decoder[TestObj] = deriveDecoder
  }
  val jsonStr =
    """
      |{
      |  "cTime": -9223372036854776000
      |}
    """.stripMargin
  lazy val jsonEth = parser.parse(jsonStr)
  "the Long decoder" should {
    "decode Long.MaxValue as JsonNumber" in {
      parser.decode[JsonNumber](s"${Long.MaxValue}") mustBe a[Right[_,JsonNumber]]
    }
    "decode Long.MinValue as JsonNumber" in {
      parser.decode[JsonNumber](s"${Long.MinValue}") mustBe a[Right[_,JsonNumber]]
    }
  }
  "the instant decoder" should {
    "parse Long.MinValue" in {
      jsonEth mustBe a[Right[_, Json]]
    }
    "decode Long.MinValue" in {
      val decodedEth = jsonEth.right.get.as[TestObj]
      decodedEth mustEqual Either.right(TestObj(SSDateTime.Instant.Min))
    }
  }

}

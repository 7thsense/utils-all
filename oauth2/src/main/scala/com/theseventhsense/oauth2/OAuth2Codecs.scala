package com.theseventhsense.oauth2

import com.theseventhsense.udatetime.UDateTimeCodecs
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.defaults._

/**
  * Created by erik on 6/22/16.
  */
trait OAuth2Codecs extends UDateTimeCodecs {
  implicit val oauth2IdEncoder: Encoder[OAuth2Id] =
    Encoder[Long].contramap(_.id)
  implicit val oauth2IdDecoder: Decoder[OAuth2Id] =
    Decoder[Long].map(OAuth2Id(_))

  implicit val oauth2CredentialEncoder: ObjectEncoder[OAuth2Credential] =
    deriveEncoder[OAuth2Credential]
  implicit val oauth2CredentialDecoder: Decoder[OAuth2Credential] =
    deriveDecoder[OAuth2Credential]

  implicit val responseDecoder: Decoder[OAuth2TokenResponse] =
    deriveDecoder[OAuth2TokenResponse]
  implicit val responseEncoder: ObjectEncoder[OAuth2TokenResponse] =
    deriveEncoder[OAuth2TokenResponse]

}

object OAuth2Codecs extends OAuth2Codecs

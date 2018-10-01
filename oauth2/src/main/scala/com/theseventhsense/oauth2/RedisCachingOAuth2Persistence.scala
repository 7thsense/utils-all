package com.theseventhsense.oauth2

import akka.util.ByteString
import cats.implicits._

import com.theseventhsense.utils.types.SSDateTime
import redis.{ByteStringFormatter, RedisClient}
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import scala.concurrent.{ExecutionContext, Future}

import com.theseventhsense.utils.logging.LogContext
import com.theseventhsense.utils.models.TLogContext

/**
  * Created by erik on 2/20/17.
  */
class RedisCachingOAuth2Persistence(persistence: TOAuth2Persistence,
                                    redis: RedisClient)
    extends TOAuth2Persistence {
  import RedisCachingOAuth2Persistence._
  private def idToKey(id: OAuth2Id) = s"ss::oauth2/oAuth2Credential/${id.id}"

  override def create(
    cred: OAuth2Credential
  )(implicit ec: ExecutionContext, lc: TLogContext): Future[OAuth2Credential] = {
    persistence.create(cred).flatMap { c =>
      redis.set(idToKey(c.id), c).map(_ => c)
    }
  }

  override def save(
    cred: OAuth2Credential
  )(implicit ec: ExecutionContext, lc: TLogContext): Future[OAuth2Credential] = {
    persistence.save(cred).flatMap { c =>
      redis.set(idToKey(c.id), c).map(_ => c)
    }
  }

  override def get(
    id: OAuth2Id
  )(implicit ec: ExecutionContext, lc: TLogContext): Future[Option[OAuth2Credential]] = {
    redis.get[OAuth2Credential](idToKey(id)).flatMap {
      case Some(cred) if cred.accessExpires.forall(_.isAfter(SSDateTime.now)) =>
        Future.successful(Option(cred))
      case _ =>
        persistence.get(id).flatMap {
          case Some(cred) =>
            redis.set(idToKey(cred.id), cred).map(_ => cred)
          case None =>
            Future.successful(None)
        }
    }
    persistence.get(id)
  }

  override def delete(
    id: OAuth2Id
  )(implicit ec: ExecutionContext, lc: TLogContext): Future[Int] = {
    persistence.delete(id).flatMap { x =>
      redis.del(idToKey(id)).map(_ => x)
    }
  }
}

object RedisCachingOAuth2Persistence {
  import com.theseventhsense.udatetime.UDateTimeCodecs._
  implicit val oAuth2IdEncoder: Encoder[OAuth2Id] =
    Encoder[Long].contramap(_.id)
  implicit val oAuth2IdDecoder: Decoder[OAuth2Id] =
    Decoder[Long].map(OAuth2Id(_))
  implicit val circeOAuth2CredentialEncoder: ObjectEncoder[OAuth2Credential] =
    deriveEncoder[OAuth2Credential]
  implicit val circeOAuth2CredentialDecoder: Decoder[OAuth2Credential] =
    deriveDecoder[OAuth2Credential]
  implicit val byteStringFormatter: ByteStringFormatter[OAuth2Credential] =
    new ByteStringFormatter[OAuth2Credential] {
      def serialize(data: OAuth2Credential): ByteString =
        ByteString(data.asJson.noSpaces)

      def deserialize(bs: ByteString): OAuth2Credential =
        parser.decode[OAuth2Credential](bs.utf8String) match {
          case Left(error)             => throw error
          case Right(oAuth2Credential) => oAuth2Credential
        }
    }
}

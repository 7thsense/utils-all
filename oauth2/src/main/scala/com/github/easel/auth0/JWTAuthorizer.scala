package com.github.easel.auth0

import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

import cats.data.NonEmptyList
import cats.implicits._
import com.auth0.jwk._
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions._
import com.auth0.jwt.interfaces.{Claim, DecodedJWT}
import com.auth0.jwt.{JWT, JWTVerifier}
import com.theseventhsense.utils.logging.Logging

import scala.collection.concurrent
import scala.collection.JavaConverters._
import scala.util.Try

object JWTAuthorizer {

  sealed abstract class Auth0Error extends Throwable

  object Auth0Error {

    sealed abstract class DecodeError extends Auth0Error

    object DecodeError {

      case class InvalidIssuer(token: String,
                               issuer: String,
                               override val getMessage: String)
          extends DecodeError

      case class DecodeFailed(token: String, cause: JWTDecodeException)
          extends DecodeError {
        override def getMessage: String = cause.getMessage
      }

      case class Unknown(token: String, cause: Throwable) extends DecodeError {
        override def getMessage: String = cause.getMessage
      }

    }

    sealed abstract class VerifyError extends Auth0Error {
      def cause: Throwable

      override def getMessage: String = cause.getMessage
    }

    object VerifyError {

      case class SigningKeyNotFound(override val getMessage: String,
                                    cause: SigningKeyNotFoundException)
          extends VerifyError

      case class AlgorithmMismatch(token: String,
                                   cause: AlgorithmMismatchException)
          extends VerifyError

      case class SignatureVerification(token: String,
                                       cause: SignatureVerificationException)
          extends VerifyError

      case class InvalidClaim(token: String, cause: InvalidClaimException)
          extends VerifyError

      case class TokenExpired(token: String, cause: TokenExpiredException)
          extends VerifyError

      case class Unknown(token: String, cause: Throwable) extends VerifyError

    }

    sealed abstract class ExtractError extends Auth0Error

    object ExtractError {

      case class Missing(token: String, claim: String) extends ExtractError {
        override def getMessage: String = s"Missing claim: $claim"
      }

      case class MissingSubject(token: String) extends ExtractError {
        override def getMessage: String = s"Missing subject"
      }

      case class Invalid(token: String, claim: String, value: Any)
          extends ExtractError {
        override def getMessage: String = s"Invalid claim: $claim was $value"
      }

    }

  }

  case class IdToken(token: String,
                     sub: String,
                     claims: Map[String, Claim] = Map.empty)

}

class JWTAuthorizer(requiredAudiences: Option[NonEmptyList[String]] = None,
                    requiredIssuer: Option[String] = None)
    extends Logging {

  import JWTAuthorizer.Auth0Error._
  import JWTAuthorizer._

  private val jwkProviders: concurrent.TrieMap[String, JwkProvider] =
    concurrent.TrieMap.empty

  /*
   * Construct a jwk provider, short-circuiting the build process for google accounts
   * since they don't publish their jwks.keys at the normal location.
   */
  private def buildJwkProvider(issuer: String): JwkProvider = {
    if (issuer == "accounts.google.com") {
      var urlProvider: JwkProvider = new UrlJwkProvider(
        new URL("https://www.googleapis.com/oauth2/v3/certs")
      )
      urlProvider =
        new GuavaCachedJwkProvider(urlProvider, 10, 24, TimeUnit.HOURS)
      urlProvider
    } else
      new JwkProviderBuilder(issuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
  }

  def jwkProvider(issuer: String): JwkProvider =
    jwkProviders.getOrElseUpdate(issuer, buildJwkProvider(issuer))

  private val BearerString = "Bearer "

  /**
    * Decode a token string, stripping the Bearer and returning
    * a detailed error message in case of failure.
    *
    * @param token The token string, optionally prefixed with `BearerString`
    * @return Either a decoderror, or the decoded jwt
    */
  private def decode(token: String): Either[DecodeError, DecodedJWT] = {
    val strippedToken = if (token.startsWith(BearerString)) {
      token.replace(BearerString, "")
    } else token
    Either
      .fromTry(Try(JWT.decode(strippedToken)))
      .leftMap {
        case e: JWTDecodeException =>
          DecodeError.DecodeFailed(strippedToken, e)
        case t: Throwable =>
          DecodeError.Unknown(strippedToken, t)
      }
      .right
      .flatMap {
        case jwt
            if requiredIssuer.isDefined && !requiredIssuer.contains(
              jwt.getIssuer
            ) =>
          Either.left(
            DecodeError.InvalidIssuer(
              strippedToken,
              jwt.getIssuer,
              s"Invalid issuer, expected ${requiredIssuer.get} got ${jwt.getIssuer}"
            )
          )
        case jwt =>
          Either.right(jwt)
      }
  }

  private def verifier(
    issuer: String,
    kid: String
  ): Either[VerifyError.SigningKeyNotFound, JWTVerifier] =
    Either
      .fromTry(Try(jwkProvider(issuer).get(kid)))
      .left
      .map {
        case e: SigningKeyNotFoundException =>
          VerifyError.SigningKeyNotFound(
            s"No provider found for id $kid with issuer $issuer",
            e
          )
      }
      .right
      .map { jwk =>
        val publicKey = jwk.getPublicKey
        val algorithm =
          Algorithm.RSA256(publicKey.asInstanceOf[RSAPublicKey], null)
        val verifier1 = JWT
          .require(algorithm)
          .acceptLeeway(1)
          .withIssuer(issuer)
        val verifier2 = requiredAudiences match {
          case Some(audiences) =>
            verifier1.withAudience(audiences.toList: _*)
          case None =>
            verifier1
        }
        verifier2.build()
      }

  private def extract(jwt: DecodedJWT): Either[ExtractError, IdToken] =
    for {
      subject <- Either.fromOption(
        Option(jwt.getSubject),
        ExtractError.MissingSubject(jwt.getToken)
      )
    } yield
      IdToken(
        token = jwt.getToken,
        sub = subject,
        claims = jwt.getClaims.asScala.toMap
      )

  private def verify(verifier: JWTVerifier,
                     token: String): Either[VerifyError, DecodedJWT] = {
    Either
      .fromTry(Try(verifier.verify(token)))
      .left
      .map {
        case e: AlgorithmMismatchException =>
          VerifyError.AlgorithmMismatch(token, e)
        case e: SignatureVerificationException =>
          VerifyError.SignatureVerification(token, e)
        case e: InvalidClaimException =>
          VerifyError.InvalidClaim(token, e)
        case e: TokenExpiredException =>
          VerifyError.TokenExpired(token, e)
        case t: Throwable =>
          VerifyError.Unknown(token, t)
      }
  }

  def verify(token: String): Either[Auth0Error, IdToken] =
    for {
      unverified <- decode(token)
      verifier <- verifier(unverified.getIssuer, unverified.getKeyId)
      verified <- verify(verifier, unverified.getToken)
      extracted <- extract(verified)
    } yield extracted

}

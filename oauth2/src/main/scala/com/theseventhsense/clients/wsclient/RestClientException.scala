package com.theseventhsense.clients.wsclient

/**
  * Created by erik on 6/29/17.
  */
sealed abstract class RestClientException extends Throwable with Product {
  def url: String
}

object RestClientException {
  case class Forbidden(override val url: String, message: String)
      extends RestClientException {
    override def getMessage: String = s"403 forbidden requesting $url: $message"
  }

  case class DecodeFailure(override val url: String,
                           body: String,
                           message: String)
      extends RestClientException {
    override def getMessage: String =
      s"Failure decoding response from $url: $message"
  }

  case class Unknown(override val url: String, message: String)
      extends RestClientException {
    override def getMessage: String = s"Unknown error requesting $url: $message"
  }

}

package com.theseventhsense.utils

import com.theseventhsense.utils.logging.Logging

/**
  * Created by erik on 7/21/16.
  */
object Verifications extends Logging {
  class VerificationError(message: String) extends AssertionError(message)
  lazy val verificationsEnabled = sys.props
    .get("verifications.enabled")
    .orElse(sys.env.get("VERIFICATIONS_ENABLED"))
    .map(_ != "false")
    .getOrElse(true)
  def verify(criteria: => Boolean, message: => String): Unit = {
    if (!criteria) {
      val msg = s"verification failed: $message"
      if (verificationsEnabled) {
        throw new VerificationError(msg)
      } else {
        logger.warn(msg)
      }
    }
  }

}

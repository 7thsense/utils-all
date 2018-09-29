package com.theseventhsense.utils

import com.theseventhsense.utils.logging.Logging
import com.theseventhsense.utils.models.TLogContext

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

  def verify(criteria: => Boolean, message: => String)(implicit lc: TLogContext): Unit = {
    if (!criteria) {
      val msg = s"verification failed: $message"
      logger.warn(msg)
      if (verificationsEnabled) {
        throw new VerificationError(msg)
      }
    }
  }

}

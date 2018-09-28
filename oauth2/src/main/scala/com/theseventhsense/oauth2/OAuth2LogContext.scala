package com.theseventhsense.oauth2

import com.theseventhsense.utils.logging.LogContext

private[oauth2] case class OAuth2LogContext(
  context: Map[String, String] = Map.empty,
  shouldLog: Any => Boolean = _ => false
) extends LogContext

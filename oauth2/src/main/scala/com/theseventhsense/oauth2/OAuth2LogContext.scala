package com.theseventhsense.oauth2

import com.theseventhsense.utils.models.TLogContext

private[oauth2] case class OAuth2LogContext(
  context: Map[String, String] = Map.empty,
  shouldLog: Any => Boolean = _ => false
) extends TLogContext

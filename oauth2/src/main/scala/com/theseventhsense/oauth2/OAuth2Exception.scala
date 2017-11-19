package com.theseventhsense.oauth2

import play.api.PlayException

//scalastyle:off
class OAuth2Exception(title: String,
                      description: String = "",
                      cause: Throwable = null)
    extends PlayException(title, description, cause)

package com.theseventhsense.oauth2

import com.theseventhsense.utils.persistence.db.BaseId

/**
  * Created by erik on 5/4/16.
  */
case class OAuth2Id(id: Long) extends AnyVal with BaseId

object OAuth2Id {
  val Default = OAuth2Id(-1)
}

package com.theseventhsense.oauth2

import javax.inject.Inject

import com.theseventhsense.oauth2.CacheStateMapper._
import com.theseventhsense.utils.oauth2.models.OAuth2State
import play.api.cache.SyncCacheApi

class CacheStateMapper @Inject()(cacheApi: SyncCacheApi) extends TOAuth2StateMapper {
  private def idForCredential(credentialId: OAuth2Id): Option[String] = {
    cacheApi.get[String](credentialIdKey(credentialId))
  }

  override def getById(id: String): Option[OAuth2State] = {
    cacheApi.get[OAuth2State](idKey(id))
  }

  override def getByOAuth2Id(credentialId: OAuth2Id): Option[OAuth2State] = {
    for {
      id <- idForCredential(credentialId)
      state <- getById(id)
    } yield state
  }

  override def save(state: OAuth2State): Unit = {
    state.oAuth2Id.foreach { credentialId =>
      val key = credentialIdKey(credentialId)
      cacheApi.set(key, state.id)
    }
    cacheApi.set(idKey(state.id), state)
  }

  override def delete(id: String): Unit = {
    getById(id).foreach { cred =>
      cred.oAuth2Id.foreach { credentialId =>
        cacheApi.remove(credentialIdKey(credentialId))
      }
      cacheApi.remove(idKey(id))
    }
  }
}

object CacheStateMapper {
  def idKey(id: String): String = {
    s"oauth2.states.byId.$id"
  }
  def credentialIdKey(credentialId: OAuth2Id): String = {
    s"oauth2.states.byCredentialId.${credentialId.id}"
  }
}

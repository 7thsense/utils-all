package com.theseventhsense.oauth2

import java.util.UUID

import com.theseventhsense.utils.oauth2.models.OAuth2State


trait TOAuth2StateMapper {
  def getByOAuth2Id(id: OAuth2Id): Option[OAuth2State]
  def getById(id: String): Option[OAuth2State]
  def save(stateMap: OAuth2State): Unit
  def delete(id: String): Unit

  def create(
    provider: OAuth2Provider,
    oAuth2Id: Option[OAuth2Id] = None,
    next: Option[String] = None,
    bindUrl: Option[String] = None,
    oAuth2Override: Option[OAuth2CredentialsOverride] = None
  ): OAuth2State = {
    val state = OAuth2State(
      id = UUID.randomUUID().toString,
      oAuth2Id = oAuth2Id,
      providerName = provider.name,
      next = next,
      bindUrl = bindUrl,
      oAuth2Override = oAuth2Override
    )
    save(state)
    state
  }
}

@javax.inject.Singleton
class MemoryStateMapper extends TOAuth2StateMapper {
  var states: List[OAuth2State] = List.empty

  override def getByOAuth2Id(oAuth2Id: OAuth2Id): Option[OAuth2State] = {
    states.find(_.oAuth2Id.contains(oAuth2Id))
  }

  override def getById(id: String): Option[OAuth2State] = {
    states.find(_.id == id)
  }

  override def save(stateMap: OAuth2State): Unit = {
    states = states :+ stateMap
  }

  override def delete(id: String): Unit = {
    states = states.filter(_.id != id)
  }
}

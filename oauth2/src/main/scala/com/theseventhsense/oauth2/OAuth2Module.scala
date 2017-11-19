package com.theseventhsense.oauth2

import com.google.inject.Provides
import com.google.inject.name.Names
import com.theseventhsense.oauth2.OAuth2Provider._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration

class OAuth2Module extends ScalaModule {
  @Provides
  def provideOAuth2Providers(
    configuration: Configuration
  ): Set[OAuth2Provider] =
    OAuth2Config
      .oauth2Config(configuration.underlying)
      .providers
      .map(_.provider)

  override def configure(): Unit = {
    bind[IdProvider].to[RemoteAddressIdProvider]
    bind[TOAuth2Persistence].to[InMemoryOAuth2Persistence]
    bind[TOAuth2StateMapper].to[MemoryStateMapper]
    bindConstant()
      .annotatedWith(Names.named(COOKIE_NAME_KEY))
      .to(DEFAULT_STATE_COOKIE_NAME)
    bindConstant()
      .annotatedWith(Names.named(CALLBACK_URL_PATH_KEY))
      .to(DEFAULT_CALLBACK_URL_PATH)
  }
}

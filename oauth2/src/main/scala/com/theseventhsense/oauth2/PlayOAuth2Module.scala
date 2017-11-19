package com.theseventhsense.oauth2

import javax.inject

import akka.actor.ActorSystem
import com.google.inject.Provides
import com.google.inject.name.Names
import com.theseventhsense.oauth2.OAuth2Provider._
import com.theseventhsense.utils.logging.Logging
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import redis.RedisClient

class PlayOAuth2Module extends ScalaModule with Logging {

  @inject.Singleton
  @Provides
  def provideRedisClient(implicit actorSystem: ActorSystem): RedisClient = {
    val config = actorSystem.settings.config
    val host = config.getString("redis.host")
    val port = config.getInt("redis.port")
    val passwordConfig = "redis.password"
    val password = config.hasPath(passwordConfig) match {
      case true  => Some(config.getString(passwordConfig))
      case false => None
    }
    val dbConfig = "redis.db"
    val db = config.hasPath(dbConfig) match {
      case true  => Some(config.getInt(dbConfig))
      case false => None
    }
    RedisClient(host, port, password = password, db = db)
  }

  @Provides
  def provideCachingOAuth2Persistence(
    slickPersistence: SlickOAuth2Persistence,
    redis: RedisClient
  )(implicit system: ActorSystem): RedisCachingOAuth2Persistence = {
    new RedisCachingOAuth2Persistence(slickPersistence, redis)
  }

  @Provides
  @javax.inject.Singleton
  def provideOAuth2Providers(
    configuration: Configuration
  ): Set[OAuth2Provider] =
    OAuth2Config
      .oauth2Config(configuration.underlying)
      .providers
      .map(_.provider)

  override def configure(): Unit = {
    //bind[IdProvider].to[RemoteAddressIdProvider]
    //    bind[OAuth2Persistence].to[SlickOAuth2Persistence]
    bind[TOAuth2Persistence]
      .to[RedisCachingOAuth2Persistence]
      .asEagerSingleton()
    bind[TOAuth2StateMapper].to[CacheStateMapper]
    bindConstant()
      .annotatedWith(Names.named(COOKIE_NAME_KEY))
      .to(DEFAULT_STATE_COOKIE_NAME)
    bindConstant()
      .annotatedWith(Names.named(CALLBACK_URL_PATH_KEY))
      .to(DEFAULT_CALLBACK_URL_PATH)
  }
}

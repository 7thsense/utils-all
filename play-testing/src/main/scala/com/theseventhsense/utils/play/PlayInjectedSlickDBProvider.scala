package com.theseventhsense.utils.play

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.{AbstractModule, Provides}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.Injector
import play.api.inject.guice.GuiceInjectorBuilder
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.jdbc.JdbcProfile

/**
  * Created by erik on 7/12/2016.
  */
trait PlayInjectedSlickDBProvider[Profile <: JdbcProfile] {
  self: HasDatabaseConfigProvider[Profile] =>
  implicit def profileManifest: Manifest[Profile]

  class TestDatabaseConfigProvider extends DatabaseConfigProvider {
    override def get[P <: BasicProfile]: DatabaseConfig[P] =
      dbConfig.asInstanceOf[DatabaseConfig[P]]
  }

  def system: ActorSystem

  override def dbConfigProvider: DatabaseConfigProvider =
    new TestDatabaseConfigProvider

  class DatabaseModule(system: ActorSystem) extends AbstractModule {

    override def configure(): Unit = {
      bind(classOf[DatabaseConfigProvider]).toInstance(dbConfigProvider)
      bind(classOf[ActorSystem]).toInstance(system)
      bind(classOf[Configuration]).toInstance(Configuration())
      bind(classOf[DatabaseConfig[Profile]])
        .toInstance(dbConfigProvider.get[Profile])
    }

    @Provides
    def provideActorSystem(implicit actorSystem: ActorSystem): Materializer =
      ActorMaterializer()

//    @Provides
//    def provideDbConfig(
//      databaseConfigProvider: DatabaseConfigProvider
//    ): DatabaseConfig[Profile] =
//      databaseConfigProvider.get[Profile]
  }

  implicit def injector: Injector =
    new GuiceInjectorBuilder().bindings(new DatabaseModule(system)).build()

}

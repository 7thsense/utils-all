package com.theseventhsense.testing.slick

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.{Guice, Injector, Provides}
import com.theseventhsense.utils.persistence.db.HasDatabaseConfig
import com.typesafe.config.{Config, ConfigFactory}
import net.codingwell.scalaguice.{InjectorExtensions, ScalaModule}
import slick.basic.DatabaseConfig
import slick.basic.BasicProfile

/**
  * Created by erik on 12/1/16.
  */
trait GuiceInjectedSlickDBProvider[P <: BasicProfile] {
  self: HasDatabaseConfig[P] ⇒
  implicit def mf: Manifest[DatabaseConfig[P]]

  class DatabaseModule(system: ActorSystem) extends ScalaModule {

    override def configure(): Unit = {
      bind(classOf[ActorSystem]).toInstance(system)
      bind[DatabaseConfig[P]].toInstance(dbConfig)
      bind(classOf[Config]).toInstance(ConfigFactory.empty)
    }

    @Provides
    def provideActorSystem(implicit actorSystem: ActorSystem): Materializer = {
      ActorMaterializer()
    }

  }

  def system: ActorSystem

  implicit class ScalaGuiceInjector(i: Injector)
      extends InjectorExtensions.ScalaInjector(i)
  implicit lazy val injector: Injector =
    Guice.createInjector(new DatabaseModule(system))

}

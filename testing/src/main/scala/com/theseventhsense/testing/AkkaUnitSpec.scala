package com.theseventhsense.testing

import java.util

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext

abstract class AkkaUnitSpec(roles: Seq[String] = Seq.empty)
    extends TestKit(ActorSystem("test", AkkaUnitSpec.config(roles)))
    with ImplicitSender
    with UnitSpec {
  implicit lazy val materializer: Materializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContext = ExecutionContext.global

  val cluster = Cluster(system)
  cluster.join(cluster.selfAddress)
}

object AkkaUnitSpec {
  def config(roles: Seq[String]): Config = {
    val configMap: util.Map[String, Object] = mutable
      .Map(
        "akka.actor.provider" -> "akka.cluster.ClusterActorRefProvider",
        "akka.cluster.jmx.multi-mbeans-in-same-jvm" -> "on",
        "akka.cluster.metrics.enabled" -> "off",
        "akka.cluster.roles" -> roles.asJava,
        "akka.loggers" -> Seq("akka.event.slf4j.Slf4jLogger").asJava,
        "akka.logging-filter" -> "akka.event.slf4j.Slf4jLoggingFilter",
        "akka.persistence.journal.plugin" -> "inmemory-journal",
        "akka.persistence.snapshot-store.plugin" -> "inmemory-snapshot-store",
        "akka.remote.netty.tcp.hostname" -> "127.0.0.1",
        "akka.remote.netty.tcp.port" -> "0"
      )
      .asJava
    ConfigFactory.load().withFallback(ConfigFactory.parseMap(configMap))
  }

}

package com.theseventhsense.testing

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{ImplicitSender, TestKit}

import scala.concurrent.ExecutionContext

abstract class AsyncAkkaUnitSpec(roles: Seq[String] = Seq.empty)
    extends TestKit(ActorSystem("test", AkkaUnitSpec.config(roles)))
    with ImplicitSender
    with AsyncUnitSpec {
  implicit lazy val materializer: Materializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContext = system.dispatcher

//  val cluster = Cluster(system)
//  cluster.join(cluster.selfAddress)
}

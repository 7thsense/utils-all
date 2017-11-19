package com.theseventhsense.utils.spark

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver

import scala.util.{Failure, Success}

/**
  * Created by erik on 1/12/17.
  */
class AkkaStreamReceiver[A, B](source: () => Source[A, B])
    extends Receiver[A](StorageLevel.OFF_HEAP) {
  private implicit val system = ActorSystem("AkkaStreamReceiver")
  private implicit val mat = ActorMaterializer()
  private implicit val ec = system.dispatcher

  override def onStart(): Unit = {
    source().runForeach(item ⇒ store(item)).onComplete {
      case Success(_) ⇒
        stop("All items read")
      case Failure(err) ⇒
        stop("Failed reading items", err)
    }
  }

  override def onStop(): Unit = {
    system.terminate.map(_ ⇒ stop("onStop called"))
    ()
  }
}

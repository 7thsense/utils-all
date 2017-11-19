package com.theseventhsense.utils.spark

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Source, StreamConverters}
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd.RDD
import org.apache.spark.{Dependency, Partition, SparkContext, TaskContext}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.reflect.ClassTag
import scala.concurrent.duration._

/**
  * Created by erik on 1/12/17.
  */
class SourceRDD[T: ClassTag, M](source: (ActorSystem) ⇒ Source[T, M],
                                @transient private var _sc: SparkContext,
                                deps: Seq[Dependency[_]] = Seq.empty)
    extends RDD[T](_sc, deps) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[T] = {
    implicit val actorSystem: ActorSystem = ActorSystem(context.taskAttemptId().toString)
    implicit val mat: Materializer = ActorMaterializer()
    context.addTaskCompletionListener { _ =>
      Await.result(actorSystem.terminate(), 10.seconds)
      ()
    }
    source(actorSystem).runWith(StreamConverters.asJavaStream()).iterator.asScala
  }

  override protected def getPartitions: Array[Partition] =
    Array(SlickPartition(0))
}

case class SlickPartition(index: Int) extends Partition()

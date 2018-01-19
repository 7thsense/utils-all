package com.theseventhsense.clients.wsclient

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.theseventhsense.testing.AkkaUnitSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import scala.collection.immutable
import scala.concurrent.Future

class BatchLoaderSpec
    extends AkkaUnitSpec
    with ScalaFutures
    with IntegrationPatience {
  final val BATCH_SIZE = 2

  case class A(id: Int) extends KeyedTimestamp {
    def key: String = id.toString
  }

  val As = Seq(A(1), A(2), A(3))

  case class B(id: Int, aId: Int) extends KeyedTimestamp {
    def key: String = id.toString
  }

  val Bs = Seq(
    B(1, 1),
    B(2, 1),
    B(3, 2),
    B(8, 3),
    B(4, 2),
    B(5, 2),
    B(6, 3),
    B(7, 3),
    B(9, 4)
  )
  val BsWithAs =
    List(B(1, 1), B(2, 1), B(3, 2), B(4, 2), B(5, 2), B(8, 3), B(6, 3), B(7, 3))

  case class ABatch(nextOffset: Option[String], items: Seq[A]) extends Batch[A]

  class ALoader extends ParentBatchLoader[A, B] {

    override def childLoaders(a: A) = immutable.Seq(new BLoader(a))

    override def load(offsetStr: Option[String]): Future[Batch[A]] = Future {
      val offset = offsetStr.map(_.toInt).getOrElse(0)
      val tail = As.drop(offset)
      val items = tail.take(BATCH_SIZE)
      val nextOffset = if (items.length < tail.length) {
        Some((offset + items.length).toString)
      } else {
        None
      }
      ABatch(nextOffset, items)
    }

  }

  case class BBatch(nextOffset: Option[String], items: Seq[B]) extends Batch[B]

  class BLoader(a: A) extends BatchLoader[B] {
    override def load(offsetStr: Option[String]): Future[Batch[B]] = Future {
      val offset = offsetStr.map(_.toInt).getOrElse(0)
      val tail = Bs.filter(_.aId == a.id).drop(offset)
      val items = tail.take(BATCH_SIZE)
      val nextOffset = if (items.length < tail.length) {
        Some((offset + items.length).toString)
      } else {
        None
      }
      BBatch(nextOffset, items)
    }
  }

  "the test loaders" should {
    val loader = new ALoader()
    "be able to load A's" in {
      loader.load(None).futureValue mustEqual ABatch(Some("2"), Seq(A(1), A(2)))
      loader.load(Some("2")).futureValue mustEqual ABatch(None, Seq(A(3)))
    }
    "be able to load B's" in {
      val bLoader = new BLoader(As.head)
      bLoader.load(None).futureValue mustEqual BBatch(
        None,
        Seq(B(1, 1), B(2, 1))
      )
    }
  }

  "the batch loader" should {
    implicit val mat = ActorMaterializer()
    val loader = new ALoader()
    "be able to construct an iterator" in {
      val i = loader.iterator
      i.toSeq mustEqual As
    }
    "be able to combine iterators" in {
      loader.childIterator.toList.length mustEqual BsWithAs.length
    }
    "be able to construct a source" in {
      val aseq = loader.source.runWith(Sink.seq).futureValue
      aseq mustEqual As
    }
    "be able to construct a combined stream" in {
      var bseq: Seq[B] = Seq.empty
      val complete = loader.childSource.runForeach { b: B =>
        bseq :+= b
      }
      complete.futureValue
      bseq.length mustEqual BsWithAs.length
    }
  }
}

package com.theseventhsense.utils.persistence.db

import java.sql.SQLException

import akka.stream.scaladsl.Sink
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import com.theseventhsense.testing.slick.{OnePostgresDBPerSuite, SlickSpec}
import com.theseventhsense.utils.logging.LogContext
import com.theseventhsense.utils.models.TLogContext
import com.theseventhsense.utils.persistence.QueryMeta

case class TestId(id: Long) extends AnyVal with BaseId

object TestId {
  val Default = TestId(-1)
}
case class TestDTO(id: TestId, value: String)
    extends Identified[TestId, TestDTO] {
  override def withId(id: TestId): TestDTO = copy(id = id)
}

trait TestTable[P <: JdbcProfile] extends SlickTable[P] {

  import profile.api._

  implicit val testIdColumnType: BaseColumnType[TestId] =
    MappedColumnType.base[TestId, Long](
      wrappedId ⇒ wrappedId.id,
      id ⇒ TestId(id)
    )

  class Tests(tag: Tag)
      extends Table[TestDTO](tag, "test_table")
      with IdentifiedTable[TestId, TestDTO] {
    def id = column[TestId]("id", O.PrimaryKey, O.AutoInc)

    def value = column[String]("value")

    def * = (id, value) <> ((TestDTO.apply _).tupled, TestDTO.unapply)
  }

  def tests = TableQuery[Tests]
}

class TestDAO(override val dbConfig: DatabaseConfig[CustomPostgresDriver])
    extends SlickDAO[CustomPostgresDriver, TestId, TestDTO]
    with TestTable[CustomPostgresDriver] {

  override type Items = Tests
  override val table = tests
  override val idColumnType = testIdColumnType
}

class SlickDAOSpec extends SlickSpec[CustomPostgresDriver] with OnePostgresDBPerSuite {
  val ManyItemsCount = 100
  lazy val dao = new TestDAO(dbConfig)
  lazy val item1 = dao.create(TestDTO(TestId.Default, "First value")).fValue
  val itemsToCreate = Seq(
    TestDTO(TestId.Default, "Second value"),
    TestDTO(TestId.Default, "Third value"),
    TestDTO(TestId.Default, "Fourth value")
  )
  var highId = 100L
  val highItemsToCreate = itemsToCreate.map(item ⇒
    item.copy(id = {
      highId += 1
      TestId(highId)
    }))
  lazy val items = dao.createBulk(itemsToCreate).fValue

  val manyItemsToCreate = 0 until ManyItemsCount map { i =>
    TestDTO(TestId.Default, s"Value $i")
  }

  private def max(a: Long, b: Long) = if (a > b) a else b

  implicit val lc: TLogContext = LogContext.empty

  "the SlickDAO" should {
    "be able to create a value" in {
      item1 mustEqual TestDTO(TestId(1), "First value")
    }
    "be able to get a value" in {
      dao.get(item1.id).fValue mustEqual item1
    }
    "be able to delete a value" in {
      dao.delete(item1.id).fValue
    }
    "be able to create multiple values" in {
      items.length mustEqual itemsToCreate.length
    }
    "be able to query the next id" in {
      db.run(dao.currSequenceValueCommand)
        .fValue mustEqual items.reverse.head.id.id
    }
    "be able to reset the next id" in {
      db.run(dao.resetSequenceCommand(10)).fValue mustEqual 10
      db.run(dao.currSequenceValueCommand).fValue mustEqual 10
    }
    "be able to reset the sequence if necessary" in {
      db.run(dao.resetSequenceIfNecessaryCommand(9)).fValue mustEqual 10
      db.run(dao.resetSequenceIfNecessaryCommand(15)).fValue mustEqual 15
    }
    "be able to delete multiple values" in {
      dao.deleteBulk(items.map(_.id)).fValue mustEqual items.length
    }
    "be able to forceInsert a value" in {
      dao
        .forceInsert(highItemsToCreate.head)
        .fValue mustEqual highItemsToCreate.head
    }
    "refuse to forceInsert a value that already exists" in {
      dao
        .forceInsert(highItemsToCreate.head)
        .failed
        .fValue mustBe an[SQLException]
    }
    "be able to forceInsertOrUpdate a value that already exists" in {
      val modified = highItemsToCreate.head.copy(value = "modified value")
      dao.forceInsertOrUpdate(modified).fValue mustEqual modified
      dao.get(modified.id).fValue mustEqual modified
    }

    "be able to forceInsertOrUpdate multiple new values" in {
      logger.trace(s"ForceInsertOrUpdateBulk on $highItemsToCreate")
      dao
        .forceInsertOrUpdateBulk(highItemsToCreate.toSet)
        .fValue mustEqual highItemsToCreate.toSet
      dao.currSequenceValue.fValue mustEqual highItemsToCreate
        .map(_.id.id)
        .reduce(max)
      dao
        .get(highItemsToCreate.head.id)
        .fValue mustEqual highItemsToCreate.head
      dao.get(highItemsToCreate(1).id).fValue mustEqual highItemsToCreate(1)
      dao.get(highItemsToCreate(2).id).fValue mustEqual highItemsToCreate(2)
    }
    "be able to forceInsertOrUpdate multiple values that already exist" in {
      val items = highItemsToCreate.toSeq.sortBy(_.value)
      val updates = Seq(
        items.head,
        items(1).copy(value = "modified value 1"),
        items(2).copy(value = "modified value 2")
      )
      dao.forceInsertOrUpdateBulk(updates.toSet).fValue mustEqual updates.toSet
      dao.currSequenceValue.fValue mustEqual highItemsToCreate
        .map(_.id.id)
        .reduce(max)
      dao.get(updates.head.id).fValue mustEqual updates.head
      dao.get(updates(1).id).fValue mustEqual updates(1)
      dao.get(updates(2).id).fValue mustEqual updates(2)
    }

    lazy val manyCreatedItems = dao.createBulk(manyItemsToCreate).fValue
    "be able to create many items at once" in {
      dao.count.fValue mustEqual 3
      manyCreatedItems.length mustEqual ManyItemsCount
      dao.count.fValue mustEqual ManyItemsCount + 3
    }

    val qm = QueryMeta.fromOffset(0, Some(1))

    "be able to paginate through results" in {
      0 until 3 foreach { offset =>
        dao
          .find(qm.withOffset(offset.toLong))
          .fValue
          .data
          .head
          .id
          .id mustEqual 101 + offset
      }
    }

    "be able to paginate through streamed results" in {
      dao
        .stream(qm.withOffset(0))
        .fValue
        .stream
        .runWith(Sink.head)
        .futureValue
        .id
        .id mustEqual 101
      dao
        .stream(qm.withOffset(1))
        .fValue
        .stream
        .runWith(Sink.head)
        .futureValue
        .id
        .id mustEqual 102
    }

    "limit streaming resultsets via QueryMeta.rangeEnd" in {
      dao
        .stream(qm.withOffset(1))
        .fValue
        .stream
        .grouped(100)
        .runWith(Sink.head)
        .futureValue
        .length mustEqual 1
    }
  }
}

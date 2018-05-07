package com.theseventhsense.utils.persistence.db

import com.theseventhsense.utils.logging.Logging
import com.theseventhsense.utils.persistence._
import slick.dbio
import slick.jdbc.JdbcProfile
import slick.sql.SqlStreamingAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

trait SlickDAO[P <: JdbcProfile, Id <: BaseId, T <: Identified[Id, T]]
    extends SlickBaseDAO[P, Id, T]
    with StreamingDAO[P, Id, T]
    with PaginatedDAO[P, Id, T]
    with Logging {
  import profile.api._

  lazy val sequence = s"${table.baseTableRow.tableName}_id_seq"

  def create(obj: T)(implicit ec: ExecutionContext): Future[T] = {
    val q = (table returning table.map(_.id)
      into ((item, newId) => item.withId(newId))) += obj
    db.run(q)
  }

  def createBulk(objs: Seq[T]): Future[Seq[T]] = {
    val q = (table returning table.map(_.id)
      into ((item, id) => item.withId(id))) ++= objs
    db.run(q)
  }

  /**
    * Asynchronously create this table and its dependencies. Generally
    * you will only want to use this method in tests.
    */
  def createTable: Future[Unit] = {
    val createAction = table.schema.create
    logger.info(s"Creating table:\n${createAction.statements}")
    db.run(createAction)
  }

  /**
    * Asynchronously drop this table and its dependencies. Generally
    * you will only want to use this method in tests.
    */
  def dropTable(): Future[Unit] = {
    db.run(table.schema.drop)
  }

  /**
    * Get the total number of records in this table
    *
    * @return number of records in the table
    */
  def count: Future[Long] = {
    db.run(table.length.result).map(_.toLong)
  }

  def get(id: Id): Future[Option[T]] = {
    val query = table.filter(_.id === id).result.headOption
    db.run(query)
  }

  def getOrCreate(item: T): Future[T] = {
    get(item.id).flatMap {
      case Some(existingItem) => Future.successful(existingItem)
      case None               => create(item)
    }
  }

  def delete(id: Id): Future[Int] = {
    val action = table.filter(_.id === id).delete
    db.run(action)
  }

  def deleteBulk(ids: Traversable[Id]): Future[Int] = {
    if (ids.nonEmpty) {
      val queries = ids.toSeq
        .grouped(Short.MaxValue - 1)
        .map { groupedIds =>
          table.filter(_.id inSetBind groupedIds).delete
        }
        .toSeq
      db.run(DBIO.fold(queries, 0)(_ + _))
    } else {
      Future.successful(0)
    }
  }

  // Commented out since the type bounds don't quite work out (yet). Descendant
  // classes will need a message such as this if you want to be able to insert
  // records and get back a copy with the newly set id in it.
  //  def create(obj: T): T = {
  //    db withTransaction { implicit session =>
  //      (table returning table.map(_.id)
  //        into ((item, id) => item.copy(id = id))) += obj
  //    }
  //  }

  def save(obj: T): Future[T] = {
    val q = table
      .filter(_.id === obj.id)
      .update(obj)
    db.run(q).map(_ => obj)
  }

  def forceInsert(obj: T): Future[T] = {
    val q = table.forceInsert(obj).map(_ => obj)
    db.run(q)
  }

  def insertOrUpdate(obj: T): Future[T] = {
    val q = table.insertOrUpdate(obj)
    db.run(q).map(_ => obj)
  }

  def forceInsertOrUpdate(obj: T): Future[T] =
    db.run((for {
      count <- table.filter(_.id === obj.id).length.result
      _ <- if (count == 0) {
        table.forceInsert(obj)
      } else {
        table.insertOrUpdate(obj)
      }
      _ <- resetSequenceIfNecessaryCommand(obj.id.id)
    } yield obj).transactionally)

  def insertOrUpdateBulk(objs: Seq[T]): Future[Seq[T]] = {
    val q = DBIO.sequence(objs.map(table.insertOrUpdate))
    db.run(q).map(_ => objs)
  }

  def forceInsertOrUpdateBulk(obj: Set[T]): Future[Set[T]] =
    db.run({
        val ids = obj.map(_.id)
        for {
          existingItems: Seq[T] <- DBIO.fold(
            obj
              .grouped(Short.MaxValue - 1)
              .map { objGroup =>
                val idGroup: Set[Id] = objGroup.map(_.id)
                val subQ = table.filter(_.id inSetBind idGroup).result
                subQ
              }
              .toSeq,
            Seq.empty[T]
          )(_ ++ _)
          _ <- {
            val existingIds = existingItems.map(_.id)
            logger.trace(s"Found $existingIds")
            val (itemsToUpdate, itemsToInsert) = obj.partition { item =>
              existingIds.contains(item.id)
            }
            logger.trace(s"Inserting $itemsToInsert")
            logger.trace(s"Inserting ${itemsToInsert.size} items")
            val inserts = table.forceInsertAll(itemsToInsert)
            logger.trace(s"Updating $itemsToUpdate")
            val updates = itemsToUpdate.flatMap {
              itemToUpdate =>
                existingItems.find(_.id == itemToUpdate.id).flatMap {
                  existingItem =>
                    //  logger.trace(s"Updating $existingItem -> $itemToUpdate")
                    if (itemToUpdate != existingItem) {
                      val action =
                        table
                          .filter(_.id === existingItem.id)
                          .update(itemToUpdate)
                      Option(action)
                    } else {
                      None
                    }
                }
            }.toSeq
            lazy val updateSql = updates.flatMap(_.statements)
            logger.trace(s"Updating ${updates.size} items -- $updateSql")
            DBIO.sequence(updates).zip(inserts)
          }
          maxId <- resetSequenceIfNecessaryCommand(
            if (ids.nonEmpty) ids.map(_.id).max
            else 0
          )
        } yield maxId
      }.transactionally)
      .map(_ => obj)

  def saveBulk(seq: Seq[T]): Future[Seq[T]] = {
    val q = seq map { obj =>
      table
        .filter(_.id === obj.id)
        .update(obj)
        .map(x => obj)
    }
    db.run(DBIO.sequence(q))
  }

  def createCountResult(query: Query[Items, T, Seq],
                        meta: QueryMeta): Future[Int] = {
    db.run(query.length.result)
  }

  def createDeleteResult(query: Query[Items, T, Seq]): Future[Int] = {
    db.run(query.delete)
  }

  lazy val currSequenceValueCommand = {
    sql"""SELECT last_value from "#$sequence";""".as[Long].head
  }

  lazy val maxValueCommand = table.map(_.id).max

  def currSequenceValue: Future[Long] = {
    db.run(currSequenceValueCommand)
  }

  def resetSequenceCommand(
    next: Long
  ): SqlStreamingAction[Vector[Long], Long, Effect]#ResultAction[Long,
                                                                 NoStream,
                                                                 Effect] = {
    logger.debug(s"Resetting $sequence sequence to $next")
    sql"SELECT setval('#$sequence', $next);".as[Long].head
  }

  def resetSequenceIfNecessaryCommand(
    next: Long
  ): dbio.DBIOAction[Long, NoStream, Effect with Effect] = {
    for {
      current <- currSequenceValueCommand
      reset <- if (next >= current) {
        resetSequenceCommand(next)
      } else {
        sql"SELECT $current;".as[Long].head
      }
    } yield reset
  }
}

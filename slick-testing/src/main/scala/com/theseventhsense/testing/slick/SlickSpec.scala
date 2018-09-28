package com.theseventhsense.testing.slick

import com.theseventhsense.testing.AkkaUnitSpec
import com.theseventhsense.utils.logging.{LogContext, Logging}
import com.theseventhsense.utils.persistence.db.HasDatabaseConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.IntegrationPatience
import slick.jdbc.JdbcProfile

abstract class SlickSpec[P <: JdbcProfile]
    extends AkkaUnitSpec
    with HasDatabaseConfig[P]
    with Logging
    with IntegrationPatience
    with BeforeAndAfterAll {

  def databaseName: String

  def createDb(): Unit

  def dropDb(): Unit

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    dropDb()
    createDb()
  }

  override def afterAll: Unit = {
    logger.debug(s"Shutting down test database connection to $databaseName")(LogContext.empty)
    db.close
  }
}

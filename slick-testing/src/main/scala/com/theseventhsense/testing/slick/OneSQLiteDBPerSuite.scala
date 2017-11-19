package com.theseventhsense.testing.slick

import java.io.File
import java.nio.file.Files

import com.theseventhsense.utils.persistence.db.HasDatabaseConfig
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Suite}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.util.Try

trait OneSQLiteDBPerSuite
    extends Suite
    with HasDatabaseConfig[JdbcProfile]
    with StaticDatabaseConfig[JdbcProfile]
    with BeforeAndAfterAll {
  lazy val tmpDir = Files.createTempDirectory("OneSQLiteDBPerSuite")
  lazy val dbFileName =
    s"${tmpDir.toString}/${this.getClass.getSimpleName}.sqlite.db"
  lazy val config = ConfigFactory.parseString(s"""
      |testdb = {
      |  driver = "slick.driver.SQLiteDriver$$"
      |  db {
      |    url = "jdbc:sqlite:${dbFileName}"
      |    driver = org.sqlite.JDBC
      |    connectionPool = disabled
      |    keepAliveConnection = true
      |  }
      |}
    """.stripMargin)

  assert(config != null, "Config is null")

  override def staticDbConfig: DatabaseConfig[JdbcProfile] =
    DatabaseConfig.forConfig[JdbcProfile]("testdb", config)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Try(Files.delete(new File(dbFileName).toPath))
    ()
  }
}

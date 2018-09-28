package com.theseventhsense.testing.slick

import java.io.File
import java.nio.file.Files

import com.theseventhsense.utils.logging.{LogContext, Logging}
import com.theseventhsense.utils.persistence.db.HasDatabaseConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Suite}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait OneH2DBPerSuite
    extends Suite
    with HasDatabaseConfig[JdbcProfile]
    with StaticDatabaseConfig[JdbcProfile]
    with BeforeAndAfterAll
    with Logging {
  lazy val tmpDir = Files.createTempDirectory("OneH2DBPerSuite")
  lazy val dbFileName = s"${tmpDir.toString}/" +
    this.getClass.getPackage.getName.split("\\.").reverse.head + "-" +
    this.getClass.getSimpleName + ".h2.db"
  lazy val path = new File(dbFileName).toPath

  lazy val config: Config = ConfigFactory.parseString(
    s"""
       |testdb = {
       |  driver = "slick.driver.H2Driver$$"
       |  db {
       |    url = "jdbc:h2:${dbFileName};MODE=PostgreSQL;DB_CLOSE_DELAY=0"
       |    driver = org.h2.Driver
       |    connectionPool = disabled
       |    keepAliveConnection = true
       |  }
       |}
       |
    """.stripMargin
  )
  assert(config != null, "Config is null")
  logger.debug(s"Creating H2 test database $path")(LogContext.empty)

  override def staticDbConfig: DatabaseConfig[JdbcProfile] = {
    DatabaseConfig.forConfig[JdbcProfile]("testdb", config)
  }

  override def afterAll: Unit = {
    logger.info(s"Deleting test database $path")(LogContext.empty)
    db.close()
    if (Files.exists(path)) {
      Files.delete(path)
    }
  }
}

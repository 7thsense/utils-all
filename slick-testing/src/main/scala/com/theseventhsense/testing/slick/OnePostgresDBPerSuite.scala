package com.theseventhsense.testing.slick

import java.sql.Connection

import com.theseventhsense.utils.persistence.db.{CustomPostgresDriver, HasDatabaseConfig}
import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import com.theseventhsense.utils.logging.LogContext

trait OnePostgresDBPerSuite extends HasDatabaseConfig[CustomPostgresDriver] {
  self: SlickSpec[CustomPostgresDriver] ⇒
  def mf: Manifest[DatabaseConfig[CustomPostgresDriver]] =
    manifest[DatabaseConfig[CustomPostgresDriver]]
  override protected val dbConfig: DatabaseConfig[CustomPostgresDriver] =
    dbFromUrl(url)

  //def hostname: String = "thor-ubuntu.local"
  def hostname: String = "localhost"
  def adminUser: String = "postgres"
  def adminPassword: String = "secret"

  def databaseName: String = {
    url.substring(url.lastIndexOf("/") + 1, url.length)
  }

  def baseUrl: String = {
    val s = url.substring(0, url.lastIndexOf("/") + 1)
    s
  }

  def url: String = {
    val defaultUrl = s"jdbc:postgresql://$hostname/test-" +
      this.getClass.getPackage.getName.split("\\.").reverse.head + "-" +
      this.getClass.getSimpleName
    val url = sys.env.getOrElse("TEST_DATABASE_URL", defaultUrl)
    url
  }

  def dbFromUrl(url: String): DatabaseConfig[CustomPostgresDriver] = {
    val user = sys.env.get("PG_USER").orElse(Option(adminUser))
    val pass = sys.env.get("PG_PASSWORD").orElse(Option(adminPassword))
    dbFromUrl(url, user, pass)
  }
  def dbFromUrl(url: String,
                user: Option[String],
                pass: Option[String]): DatabaseConfig[CustomPostgresDriver] = {
    val baseConfigStr =
      s"""
         |test {
         |  profile = "com.theseventhsense.utils.persistence.db.CustomPostgresDriver$$"
         |  db {
         |    connectionPool = disabled
         |    driver = "org.postgresql.Driver"
         |    url = "$url"
         |  }
         |}
      """.stripMargin
    val configStr = if (user.isDefined && pass.isDefined) {
      baseConfigStr +
        s"""
           |test.db.user = "${user.get}"
           |test.db.password = "${pass.get}"
         """.stripMargin
    } else {
      baseConfigStr
    }
    val config = ConfigFactory.parseString(configStr)
    logger.trace(s"Using test database: $config")(LogContext.empty)
    DatabaseConfig.forConfig[CustomPostgresDriver]("test", config)
  }

  def adminDb: JdbcProfile#Backend#Database =
    dbFromUrl(baseUrl, Option(adminUser), Option(adminPassword)).db

  override def createDb(): Unit = {
    logger.info(s"Creating test database $databaseName")(LogContext.empty)
    val q = s"""CREATE DATABASE "$databaseName";"""
    try {
      val conn: Connection = adminDb.source.createConnection()
      val stmt = conn.createStatement()
      stmt.execute(q)
      conn.close()
    } finally adminDb.close()
    ()
  }

  override def dropDb(): Unit = {
    logger.info(s"Dropping test database $databaseName")(LogContext.empty)
    val q = s"""DROP DATABASE IF EXISTS "$databaseName";"""
    try {
      val conn: Connection = adminDb.source.createConnection()
      val stmt = conn.createStatement()
      stmt.execute(q)
      conn.close()
    } finally adminDb.close()
    ()
  }
}

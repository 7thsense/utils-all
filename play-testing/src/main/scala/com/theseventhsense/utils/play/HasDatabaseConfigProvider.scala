package com.theseventhsense.utils.play

import com.theseventhsense.utils.persistence.db.HasDatabaseConfig
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait HasDatabaseConfigProvider[P <: JdbcProfile] extends HasDatabaseConfig[P] {

  /** The provider of a Slick `DatabaseConfig` instance.*/
  protected def dbConfigProvider: DatabaseConfigProvider
  override protected val dbConfig: DatabaseConfig[P] = dbConfigProvider.get[P]
}

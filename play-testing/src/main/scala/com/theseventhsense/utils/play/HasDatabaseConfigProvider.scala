package com.theseventhsense.utils.play

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.basic.{BasicProfile, DatabaseConfig}

trait HasDatabaseConfigProvider[P <: BasicProfile]
    extends HasDatabaseConfig[P] {

  /** The provider of a Slick `DatabaseConfig` instance.*/
  protected def dbConfigProvider: DatabaseConfigProvider
  override protected val dbConfig: DatabaseConfig[P] = dbConfigProvider.get[P]
}

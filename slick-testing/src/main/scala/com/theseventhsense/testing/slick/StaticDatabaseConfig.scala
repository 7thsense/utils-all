package com.theseventhsense.testing.slick

import com.theseventhsense.utils.persistence.db.HasDatabaseConfig
import slick.basic.DatabaseConfig
import slick.basic.BasicProfile

trait StaticDatabaseConfig[P <: BasicProfile] {
  protected def staticDbConfig: DatabaseConfig[P]
}

trait StaticHasDatabaseConfig[P <: BasicProfile]
    extends StaticDatabaseConfig[P]
    with HasDatabaseConfig[P] {
  override final val dbConfig: DatabaseConfig[P] = staticDbConfig
}

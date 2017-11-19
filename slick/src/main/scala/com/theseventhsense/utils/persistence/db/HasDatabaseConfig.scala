package com.theseventhsense.utils.persistence.db

import slick.basic.DatabaseConfig
import slick.basic.BasicProfile

trait HasDatabaseConfig[P <: BasicProfile] {
  /** The Slick database configuration. */
  protected val dbConfig: DatabaseConfig[P] // field is declared as a val because we want a stable identifier.
  /** The Slick profile extracted from `dbConfig`. */
  protected final lazy val profile: P = dbConfig.profile // field is lazy to avoid early initializer problems.
  @deprecated("Use `profile` instead of `driver`", "2.1")
  protected final lazy val driver: P = dbConfig.profile // field is lazy to avoid early initializer problems.
  /** The Slick database extracted from `dbConfig`. */
  protected final def db: P#Backend#Database = dbConfig.db
}

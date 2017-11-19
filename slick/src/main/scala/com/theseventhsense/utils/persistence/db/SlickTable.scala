package com.theseventhsense.utils.persistence.db

import com.theseventhsense.utils.logging.Logging
import slick.jdbc.JdbcProfile

trait SlickTable[P <: JdbcProfile] extends SlickImplicits[P] with Logging {
  import profile.api._

  protected trait IdentifiedTable[Id <: BaseId, T <: Identified[Id, _]] {
    self: Table[T] =>
    def id: Rep[Id]
  }
}

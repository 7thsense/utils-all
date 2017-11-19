package com.theseventhsense.utils.persistence.db

import slick.jdbc.JdbcProfile

/**
  * Created by erik on 5/4/16.
  */
trait SlickBaseDAO[P <: JdbcProfile, Id <: BaseId, T <: Identified[Id, _]]
    extends SlickTable[P] {

  import profile.api._

  implicit def idColumnType: BaseColumnType[Id]

  type Items <: Table[T] with IdentifiedTable[Id, T]

  def table: TableQuery[Items]
}

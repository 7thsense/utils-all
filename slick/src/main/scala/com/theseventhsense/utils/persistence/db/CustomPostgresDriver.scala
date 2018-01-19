package com.theseventhsense.utils.persistence.db

import com.github.tminglei.slickpg._

trait CustomPostgresDriver
    extends ExPostgresProfile
    with PgArraySupport
    with PgJsonSupport
    with PgSearchSupport
    with PgDate2Support
    with PgRangeSupport
    with PgHStoreSupport
    with PgNetSupport
    with PgLTreeSupport {
  def pgjson =
    "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  override val api = MyAPI

  object MyAPI
      extends API
      with DateTimeImplicits
      with SimpleJsonImplicits
      with SimpleArrayImplicits
      with SearchImplicits
      with SearchAssistants

}

object CustomPostgresDriver extends CustomPostgresDriver

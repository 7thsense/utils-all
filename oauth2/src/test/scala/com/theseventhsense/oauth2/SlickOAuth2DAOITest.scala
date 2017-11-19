package com.theseventhsense.oauth2

import cats.implicits._
import com.theseventhsense.testing.slick.{
  OneH2DBPerSuite,
  OnePostgresDBPerSuite,
  SlickSpec
}
import com.theseventhsense.utils.persistence.db.CustomPostgresDriver
import com.theseventhsense.utils.types.SSDateTime
import slick.jdbc.JdbcProfile

abstract class SlickOAuth2DAOITest[P <: CustomPostgresDriver]
    extends SlickSpec[P] {
  val ssNow = SSDateTime.Instant.parse("2014-01-01T00:00:00Z").toOption.value
  val dto1 = SlickOAuth2DAO.OAuth2CredentialDTO(
    OAuth2Id.Default,
    "none",
    "accessToken1",
    None,
    None,
    mTime = ssNow,
    cTime = ssNow
  )

  def manager: SlickOAuth2DAO

  s"the ${manager.getClass.getName}" should {
    "be able to create its tables" in {
      manager.createTable.futureValue
    }
  }

}

class PostgresSlickOAuth2DAOITest
    extends SlickOAuth2DAOITest[CustomPostgresDriver]
    with OnePostgresDBPerSuite {
  override def manager: SlickOAuth2DAO = new SlickOAuth2DAO(dbConfig)
}

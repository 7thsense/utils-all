package com.theseventhsense.oauth2

import javax.inject.Inject

import com.theseventhsense.oauth2.SlickOAuth2DAO._
import com.theseventhsense.utils.persistence.db._
import com.theseventhsense.utils.types.SSDateTime
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.language.implicitConversions

object SlickOAuth2DAO {

  case class OAuth2CredentialDTO(
    id: OAuth2Id,
    providerName: String,
    accessToken: String,
    accessExpires: Option[SSDateTime.Instant] = None,
    refreshToken: Option[String] = None,
    authUrl: Option[String] = None,
    tokenUrl: Option[String] = None,
    clientId: Option[String] = None,
    clientSecret: Option[String] = None,
    cTime: SSDateTime.Instant = SSDateTime.Instant.now,
    mTime: SSDateTime.Instant = SSDateTime.Instant.now
  ) extends Identified[OAuth2Id, OAuth2CredentialDTO] {
    override def withId(id: OAuth2Id): OAuth2CredentialDTO = copy(id = id)
  }

  object OAuth2CredentialDTO {
    def asDomain(dto: OAuth2CredentialDTO): OAuth2Credential = {
      OAuth2Credential(
        id = dto.id,
        providerName = dto.providerName,
        accessToken = dto.accessToken,
        accessExpires = dto.accessExpires,
        refreshToken = dto.refreshToken,
        authUrl = dto.authUrl,
        tokenUrl = dto.tokenUrl,
        clientId = dto.clientId,
        clientSecret = dto.clientSecret,
        cTime = dto.cTime,
        mTime = dto.mTime
      )
    }

    def from(c: OAuth2Credential): OAuth2CredentialDTO = {
      OAuth2CredentialDTO(
        id = c.id,
        providerName = c.providerName,
        accessToken = c.accessToken,
        accessExpires = c.accessExpires,
        refreshToken = c.refreshToken,
        authUrl = c.authUrl,
        tokenUrl = c.tokenUrl,
        clientId = c.clientId,
        clientSecret = c.clientSecret,
        cTime = c.cTime,
        mTime = c.mTime
      )
    }
  }

}

trait OAuth2CredentialsTable[P <: JdbcProfile] extends SlickTable[P] {
  import profile.api._

  implicit val OAuth2IdColumnType: BaseColumnType[OAuth2Id] =
    MappedColumnType
      .base[OAuth2Id, Long](wrappedId => wrappedId.id, id => OAuth2Id(id))

  // scalastyle:off
  class Credentials(tag: Tag)
      extends Table[OAuth2CredentialDTO](tag, "oauth2_credentials")
      with IdentifiedTable[OAuth2Id, OAuth2CredentialDTO] {
    def id = column[OAuth2Id]("id", O.PrimaryKey, O.AutoInc)
    def providerName = column[String]("provider_name")
    def accessToken = column[String]("access_token")
    def accessExpires = column[Option[SSDateTime.Instant]]("access_expires")
    def refreshToken = column[Option[String]]("refresh_token")
    def authUrl = column[Option[String]]("auth_url")
    def tokenUrl = column[Option[String]]("token_url")
    def clientId = column[Option[String]]("client_id")
    def clientSecret = column[Option[String]]("client_secret")
    def cTime = column[SSDateTime.Instant]("ctime")
    def mTime = column[SSDateTime.Instant]("mtime")

    // Every table needs a * projection with the same type as the table's type parameter
    def * =
      (
        id,
        providerName,
        accessToken,
        accessExpires,
        refreshToken,
        authUrl,
        tokenUrl,
        clientId,
        clientSecret,
        cTime,
        mTime
      ) <> ((OAuth2CredentialDTO.apply _).tupled, OAuth2CredentialDTO.unapply)
  }

  val credentials = TableQuery[Credentials]
}

class SlickOAuth2DAO @Inject()(
  override protected val dbConfig: DatabaseConfig[CustomPostgresDriver]
) extends SlickDAO[CustomPostgresDriver, OAuth2Id, OAuth2CredentialDTO]
    with OAuth2CredentialsTable[CustomPostgresDriver] {
  import profile.api._

  val idColumnType = OAuth2IdColumnType
  type Items = Credentials
  val table = TableQuery[Credentials]
}

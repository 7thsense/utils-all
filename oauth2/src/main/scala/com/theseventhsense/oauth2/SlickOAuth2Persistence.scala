package com.theseventhsense.oauth2

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import com.theseventhsense.oauth2.SlickOAuth2DAO.OAuth2CredentialDTO.{
  asDomain,
  from
}
import com.theseventhsense.utils.persistence.db.CustomPostgresDriver
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

class SlickOAuth2Persistence @Inject()(dbConfigProvider: DatabaseConfigProvider)
    extends TOAuth2Persistence {
  lazy val dao = new SlickOAuth2DAO(dbConfigProvider.get[CustomPostgresDriver])

  override def create(
    cred: OAuth2Credential
  )(implicit ec: ExecutionContext): Future[OAuth2Credential] = {
    dao.create(from(cred)).map(asDomain)
  }

  override def save(
    cred: OAuth2Credential
  )(implicit ec: ExecutionContext): Future[OAuth2Credential] = {
    dao.save(from(cred)).map(asDomain)
  }

  override def get(
    id: OAuth2Id
  )(implicit ec: ExecutionContext): Future[Option[OAuth2Credential]] = {
    OptionT(dao.get(id)).map(asDomain).value
  }

  override def delete(
    id: OAuth2Id
  )(implicit ec: ExecutionContext): Future[Int] = {
    dao.delete(id)
  }
}

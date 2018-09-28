package com.theseventhsense.oauth2

import com.theseventhsense.utils.logging.{LogContext, Logging}
import scala.concurrent.{ExecutionContext, Future}

trait TOAuth2Persistence {
  def create(cred: OAuth2Credential)(implicit ec: ExecutionContext,
                                     lc: LogContext): Future[OAuth2Credential]

  def save(cred: OAuth2Credential)(implicit ec: ExecutionContext,
                                   lc: LogContext): Future[OAuth2Credential]

  def get(id: OAuth2Id)(implicit ec: ExecutionContext,
                        lc: LogContext): Future[Option[OAuth2Credential]]

  def delete(id: OAuth2Id)(implicit ec: ExecutionContext,
                           lc: LogContext): Future[Int]
}

@javax.inject.Singleton
class InMemoryOAuth2Persistence extends TOAuth2Persistence with Logging {
  @volatile
  var id = 0L

  protected var credentials: List[OAuth2Credential] = List.empty

  override def create(
    cred: OAuth2Credential
  )(implicit ec: ExecutionContext, lc: LogContext): Future[OAuth2Credential] =
    Future.successful {
      id += 1
      val credWithId = cred.copy(id = OAuth2Id(id))
      credentials = credentials :+ credWithId
      credWithId
    }

  override def save(
    cred: OAuth2Credential
  )(implicit ec: ExecutionContext, lc: LogContext): Future[OAuth2Credential] =
    Future.successful {
      logger.debug(s"Saving credential: $cred")
      credentials = credentials.filter(_.id != cred.id) :+ cred
      cred
    }

  override def get(
    id: OAuth2Id
  )(implicit ec: ExecutionContext, lc: LogContext): Future[Option[OAuth2Credential]] =
    Future.successful {
      credentials.find(_.id == id)
    }

  override def delete(
    id: OAuth2Id
  )(implicit ec: ExecutionContext, lc: LogContext): Future[Int] = {
    get(id).map { credOpt =>
      credOpt.foreach { cred =>
        credentials = credentials diff List(cred)
      }
      if (credOpt.isDefined) 1 else 0
    }
  }
}

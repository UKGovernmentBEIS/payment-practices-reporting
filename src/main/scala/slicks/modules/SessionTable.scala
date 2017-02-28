/*
 * Copyright (C) 2017  Department for Business, Energy and Industrial Strategy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package slicks.modules

import javax.inject.Inject

import com.github.tminglei.slickpg.{PgDateSupportJoda, PgPlayJsonSupport}
import db.SessionRow
import org.joda.time.LocalDateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsObject, _}
import services.{SessionId, SessionService}
import slicks.DBBinding

import scala.concurrent.{ExecutionContext, Future}

class SessionTable @Inject()(val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends DBBinding
    with SessionService
    with SessionModule
    with PgDateSupportJoda
    with PgPlayJsonSupport
    with RowBuilders {

  import api._

  override def pgjson: String = "jsonb"

  def sessionQ(sessionId: Rep[SessionId]) = sessionTable.filter(s => s.id === sessionId && s.expiresAt >= LocalDateTime.now)

  val sessionC = Compiled(sessionQ _)

  override def find(sessionId: SessionId): Future[Option[SessionRow]] = db.run {
    sessionC(sessionId).result.headOption
  }

  override def put[T: Writes](sessionId: SessionId, key: String, value: T): Future[Unit] = db.run {
    sessionC(sessionId).result.headOption.flatMap {
      case Some(s) =>
        val newSessionData = s.sessionData + (key -> Json.toJson(value))
        sessionC(sessionId).update(s.copy(sessionData = newSessionData))

      case None =>
        val row = SessionRow(sessionId, LocalDateTime.now().plusMinutes(20), JsObject(Seq(key -> Json.toJson(value))))
        sessionTable += row
    }.transactionally.map(_ => ())
  }

  override def get[T: Reads](sessionId: SessionId, key: String): Future[Option[T]] = db.run {
    sessionC(sessionId).result.headOption.map {
      _.flatMap { row =>
        (row.sessionData \ key).validateOpt[T] match {
          case JsSuccess(t, _) => t
          case JsError(_) => None
        }
      }
    }
  }

  override def get[T: Reads](sessionId: SessionId): Future[Option[T]] = db.run {
    sessionC(sessionId).result.headOption.map {
      _.flatMap { row =>
        row.sessionData.validateOpt[T] match {
          case JsSuccess(t, _) => t
          case JsError(errs) =>
            Logger.debug(errs.toString)
            None
        }
      }
    }
  }

  override def clear(sessionId: SessionId, key: String): Future[Unit] = db.run {
    sessionC(sessionId).result.headOption.map {
      case Some(s) =>
        val newSessionData = s.sessionData - key
        sessionC(sessionId).update(s.copy(sessionData = newSessionData))

      case None => DBIO.successful(())
    }.transactionally.map(_ => ())
  }

  /**
    * Refresh the expiry time of the session to be the current time plus the
    * timeout in minutes
    */
  override def refresh(sessionId: SessionId, lifetimeInMinutes: Int): Future[Unit] = db.run {
    Logger.debug(s"asked to refresh $sessionId")
    sessionC(sessionId).result.headOption.flatMap {
      case Some(s) => sessionC(sessionId).update(s.copy(expiresAt = LocalDateTime.now.plusMinutes(lifetimeInMinutes)))
      case None => DBIO.successful(())
    }.transactionally.map(_ => ())
  }

  override def removeExpired(): Future[Unit] = db.run {
    sessionTable.filter(_.expiresAt <= LocalDateTime.now).delete.map(_ => ())
  }
}
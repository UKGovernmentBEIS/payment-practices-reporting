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

package actions

import java.util.UUID
import javax.inject.Inject

import play.api.mvc.{ActionBuilder, Request, Result}
import services.{SessionId, SessionService}

import scala.concurrent.{ExecutionContext, Future}

case class SessionRequest[A](sessionId: SessionId, request: Request[A])

/**
  * This action checks for a `sessionId` attribute on the Play session. If none is found then an id is created (as a UUID).
  * The value of this `sessionId` is lifted to a property of the `SessionRequest` so that action handlers can access it
  * easily.
  */
class SessionAction @Inject()(sessionService: SessionService)(implicit ec: ExecutionContext) extends ActionBuilder[SessionRequest] {
  override def invokeBlock[A](request: Request[A], block: (SessionRequest[A]) => Future[Result]): Future[Result] = {
    val sessionId = request.session.get("sessionId") match {
      case Some(id) => SessionId(id)
      case None => SessionId(UUID.randomUUID().toString)
    }

    sessionService.refresh(sessionId).flatMap { _ =>
      block(SessionRequest(sessionId, request)).map { result =>
        result.addingToSession("sessionId" -> sessionId.id)(request)
      }
    }
  }
}

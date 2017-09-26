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

import javax.inject.Inject

import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, Request, Result, WrappedRequest}
import services.{SessionId, SessionService}

import scala.concurrent.{ExecutionContext, Future}

case class SessionRequest[A](sessionId: SessionId, request: Request[A]) extends WrappedRequest[A](request)

/**
  * This action checks for a `sessionId` attribute on the Play session. If none is found then an id is created (as a UUID).
  * The value of this `sessionId` is lifted to a property of the `SessionRequest` so that action handlers can access it
  * easily.
  *
  * If a sessionId is found on the request, but does not exist in the SessionService, then it is assumed that the
  * session has timed out and the user is redirected to an appropriate error page.
  *
  * @param rejectOnTimeout If this is true then if the action determines that the user's existing session has timed out
  *                        then they will be redirected to a Timeout page. If false (the default), the action will create a fresh
  *                        session and continue.
  */
class SessionAction(sessionService: SessionService, rejectOnTimeout: Boolean)(implicit ec: ExecutionContext) extends ActionBuilder[SessionRequest] {

  @Inject() def this(sessionService: SessionService)(implicit ec: ExecutionContext) = this(sessionService, true)

  /**
    * Copy this `SessionAction` setting the `rejectOnTimeout` flag to false so that if it detects the user's
    * session has timeout it will create a fresh one and continue
    */
  def refreshOnTimeout = new SessionAction(sessionService, false)

  import SessionAction.sessionIdKey

  override def invokeBlock[A](request: Request[A], block: (SessionRequest[A]) => Future[Result]): Future[Result] = {
    for {
      sessionId <- retrieveOrCreateSessionId(request, sessionIdKey)
      exists <- sessionService.exists(sessionId)
      result <- if (exists) refreshAndInvoke(request, block, sessionId) else handleTimeout(request, block)
    } yield result

  }

  private def handleTimeout[A](request: Request[A], block: (SessionRequest[A] => Future[Result])): Future[Result] = {
    if (rejectOnTimeout) timeout
    else {
      Logger.debug("User's session has timed out so creating a new one")
      sessionService.newSession.flatMap(proceed(request, block, _))
    }
  }

  private val timeout = Future.successful(Redirect(controllers.routes.ErrorController.sessionTimeout()))

  private def retrieveOrCreateSessionId[A](request: Request[A], sessionIdKey: String): Future[SessionId] = {
    request.session.get(sessionIdKey) match {
      case Some(id) => Future.successful(SessionId(id))
      case None     => sessionService.newSession
    }
  }

  private def refreshAndInvoke[A](request: Request[A], block: (SessionRequest[A]) => Future[Result], sessionId: SessionId) = {
    sessionService.refresh(sessionId).flatMap { _ => proceed(request, block, sessionId) }
  }

  private def proceed[A](request: Request[A], block: (SessionRequest[A]) => Future[Result], sessionId: SessionId): Future[Result] = {
    block(SessionRequest(sessionId, request)).map { result =>
      result.addingToSession(sessionIdKey -> sessionId.id)(request)
    }
  }
}

object SessionAction {
  val sessionIdKey = "sessionId"
}
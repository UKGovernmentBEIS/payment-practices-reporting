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

package actors

import javax.inject.Inject

import akka.actor.Actor
import play.api.Logger
import services.SessionService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Schedule a process that will call the `SessionService.removeExpired` method periodically
  * to ensure that stale sessions are cleaned up.
  */
class SessionReaperActor @Inject()(sessionService: SessionService) extends Actor {
  Logger.debug(s"SessionReaperActor started")

  implicit val ec: ExecutionContext = context.dispatcher

  context.system.scheduler.schedule(1 second, 1 minute, self, 'poll)

  def receive: Receive = {
    case 'poll => sessionService.removeExpired().map { count =>
      if (count > 0) Logger.debug(s"reaped $count stale sessions")
    }
  }
}
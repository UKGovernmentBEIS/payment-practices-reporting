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

package utils

import org.scalactic.source.Position
import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent._
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Failure, Try}

trait FutureHelpers extends ScalaFutures with Assertions {
  self =>

  val defaultTimeout: PatienceConfiguration.Timeout =
    timeout(30.seconds)

  def await[A](future: Future[A])(implicit pos: Position, timeoutValue: PatienceConfiguration.Timeout = defaultTimeout): A =
  // we don't use futureValue here because it swallows exceptions
    Await.result(future, timeoutValue.value)

  def awaitException[E <: Throwable : ClassTag](future: Future[_])(implicit pos: Position, timeoutValue: PatienceConfiguration.Timeout = defaultTimeout): E =
    intercept[E](await(future))


  implicit class FutureOps[A](future: Future[A]) {
    def await(implicit pos: Position, timeout: PatienceConfiguration.Timeout = defaultTimeout): A = Try {
      self.await(future)
    }.recoverWith {
      case t => println(s"Future failed with exception: ${t.getMessage}"); Failure(t)
    }.get

    def awaitException[E <: Throwable : ClassTag](implicit pos: Position, timeoutValue: PatienceConfiguration.Timeout = defaultTimeout): E =
      self.awaitException(future)
  }
}

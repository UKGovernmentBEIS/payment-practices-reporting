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

import cats.data.OptionT
import cats.instances.future._
import models.CompaniesHouseId
import org.joda.time.LocalDateTime
import play.api.mvc.Results._
import play.api.mvc._
import services._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object CompanyAuthAction {
  val oAuthTokenKey = "oAuthToken"
  val companyDetailsKey = "companyDetails"
}

case class CompanyAuthRequest[A](sessionId: SessionId, companyDetail: CompanyDetail, oAuthToken: OAuthToken, request: Request[A]) extends WrappedRequest[A](request)

class CompanyAuthAction @Inject()(SessionAction: SessionAction, sessionService: SessionService, oAuth2Service: OAuth2Service)(implicit ec: ExecutionContext) {
  def extractTime(s: String): Option[LocalDateTime] = Try(new LocalDateTime(s.toLong)).toOption

  def apply(expectedId: CompaniesHouseId): ActionBuilder[CompanyAuthRequest] = new ActionBuilder[CompanyAuthRequest] {
    override def invokeBlock[A](request: Request[A], block: (CompanyAuthRequest[A]) => Future[Result]) =
      (SessionAction andThen refiner(expectedId)).invokeBlock(request, block)
  }

  def refiner(expectedId: CompaniesHouseId): ActionRefiner[SessionRequest, CompanyAuthRequest] = new ActionRefiner[SessionRequest, CompanyAuthRequest] {

    import CompanyAuthAction._

    override protected def refine[A](request: SessionRequest[A]): Future[Either[Result, CompanyAuthRequest[A]]] = {
      val companyRequest = for {
        companyDetails <- OptionT(sessionService.get[CompanyDetail](request.sessionId, companyDetailsKey))
        oAuthToken <- OptionT(sessionService.get[OAuthToken](request.sessionId, oAuthTokenKey))
        freshToken <- OptionT.liftF(freshenToken(oAuthToken))
      } yield CompanyAuthRequest(request.sessionId, companyDetails, freshToken, request.request)

      companyRequest.value.map {
        case Some(car) if car.companyDetail.company_number == expectedId => Right(car)
        case Some(car) => Left(Unauthorized("company id from session does not match id in url"))
        case None => Left(Unauthorized("no company details found on request"))
      }
    }
  }

  def freshenToken(oAuthToken: OAuthToken): Future[OAuthToken] = {
    if (oAuthToken.isExpired) oAuth2Service.refreshToken(oAuthToken) else Future.successful(oAuthToken)
  }
}


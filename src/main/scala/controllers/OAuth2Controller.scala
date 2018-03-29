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

package controllers

import actions.SessionAction
import cats.data.OptionT
import cats.instances.future._
import config.{PageConfig, ServiceConfig}
import javax.inject.Inject
import models.CompaniesHouseId
import play.api.Logger
import play.api.mvc._
import services._

import scala.concurrent.{ExecutionContext, Future}

class OAuth2Controller @Inject()(
  val pageConfig: PageConfig,
  val serviceConfig: ServiceConfig,
  sessionService: SessionService,
  companySearchService: CompanySearchService,
  companyAuthService: CompanyAuthService,
  SessionAction: SessionAction
)(implicit exec: ExecutionContext) extends Controller with PageHelper {

  def startOauthDance(companiesHouseId: CompaniesHouseId)(implicit request: RequestHeader): Result = {
    Redirect(companyAuthService.authoriseUrl(companiesHouseId), companyAuthService.authoriseParams(companiesHouseId))
  }

  //noinspection TypeAnnotation
  def claimCallback(code: Option[String], state: Option[String], error: Option[String], errorDescription: Option[String], errorCode: Option[String]) =
    SessionAction.async { implicit request =>
      state.map(CompaniesHouseId) match {
        case None => Future.successful(BadRequest(s"Unable to find company details for state $state"))

        case Some(companyId) =>
          val tokenDetails: Future[Either[Result, OAuthToken]] = code match {
            case None    => Future.successful(Left(BadRequest("No oAuth code provided")))
            case Some(c) => companyAuthService.convertCode(c).map {
              case Left(conversionError) =>
                Logger.info(s"Got an error trying to convert oAuth code: $conversionError")
                Left(BadRequest(page("We encountered an error during login")(home, views.html.errors.oauthProblem(companyId))))
              case Right(token)          => Right(token)
            }
          }

          import actions.CompanyAuthAction._

          tokenDetails.flatMap {
            case Left(result) => Future.successful(result)
            case Right(ref)   => {
              for {
                companyDetail <- OptionT(companySearchService.find(companyId))
                emailAddress <- OptionT(companyAuthService.emailAddress(companyId, ref))
                _ <- OptionT.liftF(sessionService.put(request.sessionId, oAuthTokenKey, ref))
                _ <- OptionT.liftF(sessionService.put(request.sessionId, companyDetailsKey, companyDetail))
                _ <- OptionT.liftF(sessionService.put(request.sessionId, emailAddressKey, emailAddress))
              } yield Redirect(controllers.routes.ReportingPeriodController.show(companyId, None))
            }.value.map {
              case Some(result) => result
              case None         => BadRequest(s"Unable to find company details for state $state")
            }
          }
      }
    }
}


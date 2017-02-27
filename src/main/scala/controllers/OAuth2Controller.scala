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

import javax.inject.Inject

import models.CompaniesHouseId
import play.api.mvc._
import services.{OAuth2Service, OAuthToken}

import scala.concurrent.{ExecutionContext, Future}

class OAuth2Controller @Inject()(oAuth2Service: OAuth2Service)(implicit exec: ExecutionContext) extends Controller {

  import config.Config._

  def startOauthDance(companiesHouseId: CompaniesHouseId)(implicit request: RequestHeader): Result = {
    val params = Map(
      "client_id" -> Seq(config.client.id),
      "redirect_uri" -> Seq(config.api.callbackURL),
      "scope" -> Seq(s"https://api.companieshouse.gov.uk/company/${companiesHouseId.id}"),
      "response_type" -> Seq("code")
    )
    Redirect(config.api.authorizeSchemeUri, params)
  }

  def claimCallback(code: Option[String], state: Option[String], error: Option[String], errorDescription: Option[String], errorCode: Option[String]) =
    Action.async { implicit request =>
      val tokenDetails: Future[Either[Result, OAuthToken]] = code match {
        case None => Future.successful(Left(BadRequest("No oAuth code")))
        case Some(c) => oAuth2Service.convertCode(c).map(Right(_))
      }

      val companyId = CompaniesHouseId("09575031")

      import actions.CompanyAuthAction._
      tokenDetails.map {
        case Left(result) => result
        case Right(ref) =>
          Redirect(controllers.routes.ReportController.file(companyId))
            .addingToSession(
              refreshToken -> ref.refreshToken,
              accessToken -> ref.accessToken,
              accessTokenExpiry -> ref.accessTokenExpiry.toDate.getTime.toString,
              companyIdHeader -> companyId.id,
              companyNameHeader -> "Well-Factored Software Ltd."
            )
      }
    }
}


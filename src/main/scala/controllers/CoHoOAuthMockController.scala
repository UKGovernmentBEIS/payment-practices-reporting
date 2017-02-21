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

import actions.CompanyAuthAction
import models.CompaniesHouseId
import play.api.mvc.{Action, Controller}
import services.CompaniesHouseAPI

import scala.concurrent.ExecutionContext

class CoHoOAuthMockController @Inject()(companiesHouseAPI: CompaniesHouseAPI)(implicit ec: ExecutionContext) extends Controller with PageHelper {

  import CompanyAuthAction._

  def login(companiesHouseId: CompaniesHouseId) = Action {
    Ok(views.html.oauthMock.p1(companiesHouseId))
  }

  def postLogin(companiesHouseId: CompaniesHouseId) = Action { request => Redirect(controllers.routes.CoHoOAuthMockController.authCode(companiesHouseId)) }

  def authCode(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>
    import CompanyAuthAction._
    companiesHouseAPI.find(companiesHouseId).map {
      case Some(co) =>
        Ok(views.html.oauthMock.p2(companiesHouseId, co.company_name)).addingToSession(companyNameHeader -> co.company_name)
      case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
    }
  }

  def postAuthCode(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    Redirect(controllers.routes.ReportController.file(companiesHouseId)).addingToSession(
      companyIdHeader -> companiesHouseId.id,
      accessToken -> "abcdefg",
      accessTokenExpiry -> (System.currentTimeMillis() + 4 * 60 * 1000).toString,
      refreshToken -> "hijklmnop"
    )
  }

}

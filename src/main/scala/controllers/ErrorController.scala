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

import actions.{CompanyAuthAction, SessionAction}
import config.{PageConfig, ServiceConfig}
import models.CompaniesHouseId
import play.api.mvc.{Action, Controller}

class ErrorController @Inject()(val pageConfig: PageConfig,
                                val serviceConfig: ServiceConfig,
                                CompanyAuthAction: CompanyAuthAction) extends Controller with PageHelper {

  def sessionTimeout = Action { implicit request =>
    Unauthorized(page("Your session timed out")(home, views.html.errors.sessionTimeout())).removingFromSession(SessionAction.sessionIdKey)
  }

  def invalidScope(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId) { implicit request =>
    Ok(page("Your report has not been filed because of an error")(home, views.html.errors.invalidScope(request.companyDetail)))
  }

}

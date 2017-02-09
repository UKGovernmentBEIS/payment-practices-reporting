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
import play.api.mvc.{Action, Controller}
import services.CompaniesHouseAPI

import scala.concurrent.{ExecutionContext, Future}

class SearchController @Inject()(companiesHouseAPI: CompaniesHouseAPI)(implicit ec: ExecutionContext)
  extends Controller
    with PageHelper {

  def search(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int]) = Action.async {
    query match {
      case Some(q) => companiesHouseAPI.searchCompanies(q, pageNumber.getOrElse(1), itemsPerPage.getOrElse(25)).map { results =>
        Ok(page(home, views.html.search.search(intentToFile = false, q, Some(results))))
      }
      case None => Future.successful(Ok(page(home, views.html.search.search(intentToFile = false, "", None))))
    }
  }

  def company(companiesHouseId: CompaniesHouseId, page: Option[Int]) = Action { implicit request => ??? }


}

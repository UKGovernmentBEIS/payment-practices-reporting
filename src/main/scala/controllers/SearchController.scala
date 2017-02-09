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

import play.api.mvc.{Action, Controller}
import services.CompaniesHouseAPI

import scala.concurrent.ExecutionContext

class SearchController @Inject()(companiesHouseAPI: CompaniesHouseAPI)(implicit ec: ExecutionContext)
  extends Controller
    with PageHelper {

  def doSearch(query: String, page: Option[Int], itemsPerPage: Option[Int]) = Action.async {
    companiesHouseAPI.searchCompanies(query, page.getOrElse(1), itemsPerPage.getOrElse(20)).map { results =>
      Ok(results.toString)
    }
  }


}

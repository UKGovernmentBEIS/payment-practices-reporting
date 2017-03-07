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

package services

import com.wellfactored.playbindings.ValueClassFormats
import models.CompaniesHouseId
import play.api.libs.json.Json

import scala.concurrent.Future

case class CompanySearchResult(companiesHouseId: CompaniesHouseId, companyName: String, companyAddress: String)

case class CompanyDetail(companiesHouseId: CompaniesHouseId, companyName: String)

object CompanyDetail extends ValueClassFormats {
  implicit val fmt = Json.format[CompanyDetail]
}

trait CompanySearchService {
  def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[PagedResults[CompanySearchResult]]

  def find(companiesHouseId: CompaniesHouseId): Future[Option[CompanyDetail]]
}

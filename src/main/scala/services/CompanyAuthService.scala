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

import com.google.inject.ImplementedBy
import com.wellfactored.playbindings.ValueClassFormats
import models.CompaniesHouseId
import play.api.libs.json.Json
import services.companiesHouse.CompaniesHouseAuth

import scala.concurrent.Future

case class CompanySummary(company_number: CompaniesHouseId, title: String, address_snippet: String)

case class CompanyDetail(company_number: CompaniesHouseId, company_name: String)

object CompanyDetail extends ValueClassFormats {
  implicit val fmt = Json.format[CompanyDetail]
}

case class ResultsPage(
                        page_number: Int,
                        start_index: Int,
                        items_per_page: Int,
                        total_results: Int,
                        items: List[CompanySummary]
                      )


@ImplementedBy(classOf[CompaniesHouseAuth])
trait CompanyAuthService {

  def isInScope(companiesHouseIdentifier: CompaniesHouseId, oAuthToken: OAuthToken): Future[Boolean]

  def emailAddress(oAuthToken: OAuthToken): Future[Option[String]]

  def targetScope(companiesHouseId: CompaniesHouseId): String
}


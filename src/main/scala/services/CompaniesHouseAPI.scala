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

import java.util.Base64
import javax.inject.Inject

import com.google.inject.ImplementedBy
import com.wellfactored.playbindings.ValueClassReads
import config.Config
import models.CompaniesHouseId
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

case class ResultItem(company_number: CompaniesHouseId, title: String, address_snippet: String)

case class ResultsPage(
                        page_number: Int,
                        start_index: Int,
                        items_per_page: Int,
                        total_results: Int,
                        items: List[ResultItem]
                      ) {

  val pageCount = (total_results / items_per_page.toDouble).ceil

  private def isValidRange(pageNumber: Int) = pageNumber <= pageCount && pageNumber >= 1

  def canPage: Boolean = canGoBack || canGoNext

  def canGoBack: Boolean = canGo(page_number - 1)

  def canGoNext: Boolean = canGo(page_number + 1)

  def canGo(pageNumber: Int): Boolean = isValidRange(pageNumber)
}

@ImplementedBy(classOf[CompaniesHouseAPIImpl])
trait CompaniesHouseAPI {
  def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[ResultsPage]
}

class CompaniesHouseAPIImpl @Inject()(val ws: WSClient)(implicit val ec: ExecutionContext)
  extends RestService
    with CompaniesHouseAPI
    with ValueClassReads {

  implicit val resultItemReads: Reads[ResultItem] = Json.reads[ResultItem]
  implicit val resultsPageReads: Reads[ResultsPage] = Json.reads[ResultsPage]

  override def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[ResultsPage] = {
    val s = views.html.helper.urlEncode(search)
    val startIndex = (page - 1) * itemsPerPage
    val url = s"https://api.companieshouse.gov.uk/search/companies?q=$s&items_per_page=$itemsPerPage&start_index=$startIndex"
    val basicAuth = "Basic " + new String(Base64.getEncoder.encode(Config.config.companiesHouse.apiKey.getBytes))

    get[ResultsPage](url, basicAuth)
  }
}
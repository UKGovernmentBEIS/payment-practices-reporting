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
import com.wellfactored.playbindings.{ValueClassFormats, ValueClassReads}
import config.Config
import models.CompaniesHouseId
import org.scalactic.TripleEquals._
import play.api.Logger
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

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

@ImplementedBy(classOf[CompaniesHouseAPIImpl])
trait CompaniesHouseAPI {
  def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[PagedResults[CompanySummary]]

  def find(companiesHouseId: CompaniesHouseId): Future[Option[CompanyDetail]]

  def isInScope(companiesHouseIdentifier: CompaniesHouseId, oAuthToken: OAuthToken): Future[Boolean]

  def getEmailAddress(oAuthToken: OAuthToken): Future[Option[String]]

  def targetScope(companiesHouseId: CompaniesHouseId): String
}

class CompaniesHouseAPIImpl @Inject()(val ws: WSClient, oAuth2Service: OAuth2Service)(implicit val ec: ExecutionContext)
  extends RestService
    with CompaniesHouseAPI
    with ValueClassReads {

  implicit val companySummaryReads: Reads[CompanySummary] = Json.reads[CompanySummary]
  implicit val companyDetailReads: Reads[CompanyDetail] = Json.reads[CompanyDetail]
  implicit val resultsPageReads: Reads[ResultsPage] = Json.reads[ResultsPage]

  override def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[PagedResults[CompanySummary]] = {
    val s = views.html.helper.urlEncode(search)
    val startIndex = (page - 1) * itemsPerPage
    val url = s"https://api.companieshouse.gov.uk/search/companies?q=$s&items_per_page=$itemsPerPage&start_index=$startIndex"
    val basicAuth = "Basic " + new String(Base64.getEncoder.encode(Config.config.companiesHouse.apiKey.getBytes))

    val start = System.currentTimeMillis()

    get[ResultsPage](url, basicAuth).map { resultsPage =>
      val t = System.currentTimeMillis() - start
      Logger.debug(s"Companies house search took ${t}ms")
      PagedResults(resultsPage.items, resultsPage.items_per_page, resultsPage.page_number, resultsPage.total_results)
    }
  }

  override def find(companiesHouseId: CompaniesHouseId): Future[Option[CompanyDetail]] = {
    val id = views.html.helper.urlEncode(companiesHouseId.id)
    val url = s"https://api.companieshouse.gov.uk/company/$id"
    val basicAuth = "Basic " + new String(Base64.getEncoder.encode(Config.config.companiesHouse.apiKey.getBytes))

    getOpt[CompanyDetail](url, basicAuth)
  }

  case class VerifyResult(scope: String)

  override def isInScope(companiesHouseId: CompaniesHouseId, oAuthToken: OAuthToken): Future[Boolean] = {
    implicit val verifyReads = Json.reads[VerifyResult]
    val url = "https://account.companieshouse.gov.uk/oauth2/verify"
    val auth = s"Bearer ${oAuthToken.accessToken}"
    get[VerifyResult](url, auth).map(_.scope === targetScope(companiesHouseId))
  }

  def targetScope(companiesHouseId: CompaniesHouseId): String = s"https://api.companieshouse.gov.uk/company/${companiesHouseId.id}"

  case class Email(email: String)

  implicit val emailReads = Json.reads[Email]

  override def getEmailAddress(token: OAuthToken): Future[Option[String]] = {
    val auth = s"Bearer ${token.accessToken}"
    val url = "https://account.companieshouse.gov.uk/user/profile"
    getOpt[Email](url, auth).map(_.map(_.email))
  }


  case class AccessTokenResponse(access_token: String, expires_in: Long, refresh_token: String)

  implicit val atrReads = Json.reads[AccessTokenResponse]


}
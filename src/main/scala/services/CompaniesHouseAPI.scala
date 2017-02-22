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
import org.joda.time.LocalDateTime
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

case class CompanySummary(company_number: CompaniesHouseId, title: String, address_snippet: String)

case class CompanyDetail(company_number: CompaniesHouseId, company_name: String)


case class ResultsPage(
                        page_number: Int,
                        start_index: Int,
                        items_per_page: Int,
                        total_results: Int,
                        items: List[CompanySummary]
                      )

case class ResponseWithToken[T](oAuthToken: OAuthToken, value: T)

@ImplementedBy(classOf[CompaniesHouseAPIImpl])
trait CompaniesHouseAPI {
  def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[PagedResults[CompanySummary]]

  def find(companiesHouseId: CompaniesHouseId): Future[Option[CompanyDetail]]

  def verifyAuthCode(authCode: String, redirectUri: String, companiesHouseIdentifier: String): String

  def isInScope(companiesHouseIdentifier: CompaniesHouseId, oAuthToken: OAuthToken): Future[ResponseWithToken[Boolean]]

  def getEmailAddress(oAuthToken: OAuthToken): Future[ResponseWithToken[Option[String]]]
}

class CompaniesHouseAPIImpl @Inject()(val ws: WSClient)(implicit val ec: ExecutionContext)
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

  override def verifyAuthCode(authCode: String, redirectUri: String, companiesHouseIdentifier: String): String = ???

  override def isInScope(companiesHouseIdentifier: CompaniesHouseId, oAuthToken: OAuthToken): Future[ResponseWithToken[Boolean]] = Future.successful(ResponseWithToken(oAuthToken, true))

  case class Email(email: String)

  implicit val emailReads = Json.reads[Email]

  override def getEmailAddress(token: OAuthToken): Future[ResponseWithToken[Option[String]]] = {
    withFreshAccessToken(token) { freshToken =>
      val auth = s"Bearer ${freshToken.accessToken}"
      val url = "https://account.companieshouse.gov.uk/user/profile"
      getOpt[Email](url, auth).map(e => ResponseWithToken(freshToken, e.map(_.email)))
    }
  }

  def withFreshAccessToken[T](oAuthToken: OAuthToken)(body: OAuthToken => Future[T]): Future[T] = {
    val f = if (oAuthToken.isExpired) refreshToken(oAuthToken) else Future.successful(oAuthToken)
    f.flatMap(body(_))
  }

  case class AccessTokenResponse(access_token: String, expires_in: Long, refresh_token: String)

  implicit val atrReads = Json.reads[AccessTokenResponse]

  def refreshToken(oAuthToken: OAuthToken): Future[OAuthToken] = {
    val url = "https://account.companieshouse.gov.uk/oauth2/token"
    val body = Map(
      "client_id" -> "",
      "client_secret" -> "",
      "grant_type" -> "refresh_token",
      "refresh_token" -> oAuthToken.refreshToken
    ).map { case (k, v) => (k, Seq(v)) }

    ws.url(url)
      .withHeaders(("Content-Type", "application/x-www-form-urlencoded"), ("Charset", "utf-8"))
      .post(body).map { response =>
      response.status match {
        case 200 => response.json.validate[AccessTokenResponse] match {
          case JsSuccess(atr, _) => OAuthToken(atr.access_token, LocalDateTime.now().plusSeconds(atr.expires_in.toInt), atr.refresh_token)
          case JsError(errs) => throw new Exception(errs.toString)
        }
      }
    }
  }
}
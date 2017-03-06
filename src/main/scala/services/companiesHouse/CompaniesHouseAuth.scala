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

package services.companiesHouse

import javax.inject.Inject

import config.AppConfig
import models.CompaniesHouseId
import org.scalactic.TripleEquals._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import services._

import scala.concurrent.{ExecutionContext, Future}

class CompaniesHouseAuth @Inject()(val ws: WSClient, oAuth2Service: OAuth2Service, appConfig: AppConfig)(implicit val ec: ExecutionContext)
  extends RestService with CompanyAuthService {

  private def bearerAuth(oAuthToken: OAuthToken) = s"Bearer ${oAuthToken.accessToken}"

  def targetScope(companiesHouseId: CompaniesHouseId): String = s"https://api.companieshouse.gov.uk/company/${companiesHouseId.id}"

  case class VerifyResult(scope: String)

  override def isInScope(companiesHouseId: CompaniesHouseId, oAuthToken: OAuthToken): Future[Boolean] = {
    implicit val verifyReads = Json.reads[VerifyResult]
    val url = "https://account.companieshouse.gov.uk/oauth2/verify"
    get[VerifyResult](url, bearerAuth(oAuthToken)).map(_.scope === targetScope(companiesHouseId))
  }

  case class Email(email: String)

  implicit val emailReads = Json.reads[Email]

  override def emailAddress(token: OAuthToken): Future[Option[String]] = {
    val url = "https://account.companieshouse.gov.uk/user/profile"
    getOpt[Email](url, bearerAuth(token)).map(_.map(_.email))
  }
}

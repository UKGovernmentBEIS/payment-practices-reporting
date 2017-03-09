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

package services.live

import javax.inject.Inject

import config.AppConfig
import models.CompaniesHouseId
import org.joda.time.LocalDateTime
import org.scalactic.TripleEquals._
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import services._

import scala.concurrent.{ExecutionContext, Future}

case class AccessTokenResponse(access_token: String, expires_in: Int, refresh_token: String, token_type: String)

case class RefreshTokenResponse(access_token: String, expires_in: Int)

class CompaniesHouseAuth @Inject()(val ws: WSClient, appConfig: AppConfig)(implicit val ec: ExecutionContext)
  extends RestService with CompanyAuthService {

  import appConfig.config

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

  override def emailAddress(companiesHouseId: CompaniesHouseId, token: OAuthToken): Future[Option[String]] = {
    val url = "https://account.companieshouse.gov.uk/user/profile"
    getOpt[Email](url, bearerAuth(token)).map(_.map(_.email))
  }

  override def authoriseUrl(companiesHouseId: CompaniesHouseId) = config.oAuth.authorizeSchemeUri

  override def authoriseParams(companiesHouseId: CompaniesHouseId) = Map(
    "client_id" -> Seq(config.companySearchAPI.id),
    "redirect_uri" -> Seq(config.oAuth.callbackURL),
    "scope" -> Seq(targetScope(companiesHouseId)),
    "state" -> Seq(companiesHouseId.id),
    "response_type" -> Seq("code")
  )

  import appConfig.config._

  val clientIdParam = "client_id"
  val clientSecretParam = "client_secret"
  val grantTypeParam = "grant_type"
  val redirectUriParam = "redirect_uri"
  val codeParam = "code"
  val refreshTokenParam = "refresh_token"

  val clientDetails = Map(
    clientIdParam -> companySearchAPI.id,
    clientSecretParam -> companySearchAPI.secret
  )

  implicit val atrFormat = Json.format[AccessTokenResponse]

  private[services] def mkParams(ps: Seq[(String, String)]): Map[String, Seq[String]] =
    (clientDetails ++ ps).map { case (k, v) => k -> Seq(v) }

  private[services] def call(params: Seq[(String, String)]): Future[WSResponse] = {
    ws.url(oAuth.accessTokenUri).withMethod("POST").withBody(mkParams(params)).execute()
  }


  def convertCode(code: String): Future[OAuthToken] = {
    Logger.debug("convert code")


    val params = Seq(
      grantTypeParam -> "authorization_code",
      codeParam -> code,
      redirectUriParam -> oAuth.callbackURL
    )

    call(params).map { response =>
      response.status match {
        case 200 => response.json.validate[AccessTokenResponse] match {
          case JsSuccess(resp, _) =>
            Logger.debug(s"converted code to token $resp")
            OAuthToken(resp.access_token, LocalDateTime.now().plusSeconds(resp.expires_in), resp.refresh_token)
          case JsError(errs) =>
            Logger.warn(s"response json is ${response.json}")
            throw new Exception(s"could not decode token response: $errs")
        }

        case s =>
          Logger.warn("Request to exchange code for token failed")
          Logger.warn(s"Response is $s with body: '${response.body}'")
          throw new Exception(s"Request to exchange code for token failed with ${response.body}")
      }
    }
  }

  implicit val rtrFormat = Json.format[RefreshTokenResponse]

  def refreshAccessToken(oAuthToken: OAuthToken): Future[OAuthToken] = {
    val url = "https://account.companieshouse.gov.uk/oauth2/token"

    val body = Map(
      clientIdParam -> companySearchAPI.id,
      clientSecretParam -> companySearchAPI.secret,
      grantTypeParam -> "refresh_token",
      refreshTokenParam -> oAuthToken.refreshToken
    ).map { case (k, v) => (k, Seq(v)) }

    ws.url(url)
      .withHeaders(("Content-Type", "application/x-www-form-urlencoded"), ("Charset", "utf-8"))
      .post(body).map { response =>
      response.status match {
        case 200 => response.json.validate[RefreshTokenResponse] match {
          case JsSuccess(rtr, _) =>
            Logger.debug(rtr.toString)
            OAuthToken(rtr.access_token, LocalDateTime.now().plusSeconds(rtr.expires_in - 10), oAuthToken.refreshToken)
          case JsError(errs) => throw new Exception(errs.toString)
        }

        case s =>
          Logger.warn("Request to refresh access token failed")
          Logger.warn(s"Response is $s with body: '${response.body}'")
          throw new Exception(s"Request to refresh access token failed with ${response.body}")
      }
    }
  }
}

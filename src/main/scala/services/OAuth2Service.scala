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

import javax.inject.Inject

import com.google.inject.ImplementedBy
import com.wellfactored.playbindings.ValueClassFormats
import org.joda.time.LocalDateTime
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

case class AccessTokenResponse(access_token: String, expires_in: Int, refresh_token: String, token_type: String)

case class RefreshTokenResponse(access_token: String, expires_in: Int)

@ImplementedBy(classOf[OAuth2ServiceImpl])
trait OAuth2Service {
  def convertCode(code: String): Future[OAuthToken]

  def refreshToken(oAuthToken: OAuthToken): Future[OAuthToken]
}

class OAuth2ServiceImpl @Inject()(ws: WSClient)(implicit ec: ExecutionContext) extends OAuth2Service with ValueClassFormats {

  import config.Config.config._

  val clientDetails = Map(
    "client_id" -> client.id,
    "client_secret" -> client.secret
  )

  implicit val atrFormat = Json.format[AccessTokenResponse]

  private[services] def mkParams(ps: Seq[(String, String)]): Map[String, Seq[String]] =
    (clientDetails ++ ps).map { case (k, v) => k -> Seq(v) }

  private[services] def call(params: Seq[(String, String)]): Future[WSResponse] = {
    ws.url(api.accessTokenUri).withMethod("POST").withBody(mkParams(params)).execute()
  }

  def convertCode(code: String): Future[OAuthToken] = {
    Logger.debug("convert code")
    val params = Seq(
      "grant_type" -> "authorization_code",
      "code" -> code,
      "redirect_uri" -> api.callbackURL
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

  def refreshToken(oAuthToken: OAuthToken): Future[OAuthToken] = {
    val url = "https://account.companieshouse.gov.uk/oauth2/token"
    val body = Map(
      "client_id" -> client.id,
      "client_secret" -> client.secret,
      "grant_type" -> "refresh_token",
      "refresh_token" -> oAuthToken.refreshToken
    ).map { case (k, v) => (k, Seq(v)) }

    ws.url(url)
      .withHeaders(("Content-Type", "application/x-www-form-urlencoded"), ("Charset", "utf-8"))
      .post(body).map { response =>
      response.status match {
        case 200 => response.json.validate[AccessTokenResponse] match {
          case JsSuccess(atr, _) => OAuthToken(atr.access_token, LocalDateTime.now().plusSeconds(atr.expires_in), atr.refresh_token)
          case JsError(errs) => throw new Exception(errs.toString)
        }
      }
    }
  }
}

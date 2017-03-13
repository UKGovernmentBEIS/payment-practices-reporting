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

package config

import javax.inject.Inject

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Configuration

import scala.util.Try

case class CompaniesHouseConfig(apiKey: String)

case class NotifyConfig(
                         apiKey: String,
                         templateId: String
                       )

case class OAuthConfig(host: String, callbackURL: String) {
  val baseURI = s"https://$host"

  val accessTokenUri = s"$baseURI/oauth2/token"
  val authorizeSchemeUri = s"$baseURI/oauth2/authorise"
}

case class CompanySearchAPIConfig(id: String, secret: String)

case class GoogleAnalytics(code: Option[String])

case class MockConfig(mockCompanySearch: Option[Boolean], mockCompanyAuth: Option[Boolean], mockNotify: Option[Boolean])


object MockConfig {
  val empty = MockConfig(None, None, None)
}

case class ServiceConfig(startDate: Option[LocalDate])

object ServiceConfig {
  val empty = ServiceConfig(None)
}

case class Config(
                   service: Option[ServiceConfig],
                   companiesHouse: CompaniesHouseConfig,
                   companySearchAPI: CompanySearchAPIConfig,
                   notifyService: NotifyConfig,
                   oAuth: OAuthConfig,
                   googleAnalytics: Option[GoogleAnalytics],
                   sessionTimeoutInMinutes: Option[Int],
                   logAssets: Option[Boolean],
                   logRequests: Option[Boolean],
                   printDBTables: Option[Boolean],
                   mockConfig: Option[MockConfig]
                 )

class AppConfig @Inject()(configuration: Configuration) {

  val df = DateTimeFormat.forPattern("yyyy-M-d")

  import pureconfig._
  import ConfigConvert._

  implicit val localDateConvert = ConfigConvert.stringConvert[LocalDate](s => Try(df.parseLocalDate(s)), df.print(_))

  lazy val config: Config = loadConfig[Config](configuration.underlying).get
}
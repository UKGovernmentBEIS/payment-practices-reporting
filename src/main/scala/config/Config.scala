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

import play.api.Configuration

case class CompaniesHouseConfig(apiKey: String)

case class NotifyConfig(
                         apiKey: String,
                         templateId: String
                       )

case class ApiConfig(host: String, isSandbox: Option[Boolean] = None, isLocal: Option[Boolean] = None, callbackURL: String) {
  val baseURI = s"https://$host"

  val accessTokenUri = s"$baseURI/oauth2/token"
  val authorizeSchemeUri = s"$baseURI/oauth2/authorise"
}

case class ClientConfig(id: String, secret: String)

case class GoogleAnalytics(code: Option[String])

case class Config(
                   companiesHouse: CompaniesHouseConfig,
                   notifyService: NotifyConfig,
                   logAssets: Option[Boolean],
                   logRequests: Option[Boolean],
                   printDBTables: Option[Boolean],
                   api: ApiConfig,
                   client: ClientConfig,
                   googleAnalytics: Option[GoogleAnalytics]
                 )

class AppConfig @Inject()(configuration: Configuration) {

  import pureconfig._

  lazy val config: Config = loadConfig[Config](configuration.underlying).get
}
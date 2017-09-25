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

import javax.inject.{Inject, Singleton}

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Configuration

import scala.util.Try

case class CompaniesHouseConfig(apiKey: String)

case class NotifyConfig(
  apiKey: String,
  templateId: String
)

case class OAuthConfig(host: String, callbackURL: String, clientId: String, clientSecret: String) {
  val baseURI = s"https://$host"

  val accessTokenUri     = s"$baseURI/oauth2/token"
  val authorizeSchemeUri = s"$baseURI/oauth2/authorise"
}

case class CompanySearchAPIConfig()

case class GoogleAnalyticsConfig(code: Option[String])

object GoogleAnalyticsConfig {
  val empty = GoogleAnalyticsConfig(None)
}

case class FeatureFlags(multiPageForm: Boolean)

case class ServiceConfig(
  startDate: Option[LocalDate],
  featureFlags: Option[FeatureFlags],
  logRequests: Option[Boolean],
  logAssets: Option[Boolean],
  sessionTimeoutInMinutes: Option[Int]) {
  def multiPageForm: Boolean = featureFlags.map(_.multiPageForm).getOrElse(ServiceConfig.defaultFeatureFlags.multiPageForm)
}

object ServiceConfig {
  val empty                   = ServiceConfig(None, None, None, None, None)
  val defaultServiceStartDate = new LocalDate(2017, 4, 6)
  val defaultFeatureFlags     = FeatureFlags(true)
}

case class SurveyMonkeyConfig(feedbackFormCode: Option[String])

object SurveyMonkeyConfig {
  val empty = SurveyMonkeyConfig(None)
}

case class RoutesConfig(searchHost: Option[String])

object RoutesConfig {
  val empty = RoutesConfig(None)
}

case class PageConfig(googleAnalytics: GoogleAnalyticsConfig, searchConfig: RoutesConfig, surveyMonkeyConfig: SurveyMonkeyConfig)

case class Config(
  service: Option[ServiceConfig],
  companiesHouse: Option[CompaniesHouseConfig],
  notifyService: Option[NotifyConfig],
  oAuth: Option[OAuthConfig],
  pageConfig: PageConfig
)

@Singleton
class AppConfig @Inject()(configuration: Configuration) {
  private val df = DateTimeFormat.forPattern("yyyy-M-d")

  import pureconfig._
  import ConfigConvert._

  private def load[T: ConfigConvert](path: String): Option[T] = Try {
    loadConfig[T](configuration.underlying, path).toOption
  }.toOption.flatten

  implicit val localDateConvert: ConfigConvert[LocalDate] = ConfigConvert.stringConvert[LocalDate](s => Try(df.parseLocalDate(s)), df.print(_))

  private val service                : Option[ServiceConfig]        = load[ServiceConfig]("service")
  private val companiesHouse         : Option[CompaniesHouseConfig] = load[CompaniesHouseConfig]("companiesHouse")
  private val notifyService          : Option[NotifyConfig]         = load[NotifyConfig]("notifyService")
  private val oAuth                  : Option[OAuthConfig]          = load[OAuthConfig]("oAuth")

  private val googleAnalytics   : GoogleAnalyticsConfig = load[GoogleAnalyticsConfig]("googleAnalytics").getOrElse(GoogleAnalyticsConfig(None))
  private val routesConfig      : RoutesConfig          = load[RoutesConfig]("externalRouter").getOrElse(RoutesConfig.empty)
  private val surveyMonkeyConfig: SurveyMonkeyConfig    = load[SurveyMonkeyConfig]("surveyMonkey").getOrElse(SurveyMonkeyConfig.empty)

  private val pageConfig = PageConfig(googleAnalytics, routesConfig, surveyMonkeyConfig)

  val config = Config(service, companiesHouse, notifyService, oAuth, pageConfig)
}
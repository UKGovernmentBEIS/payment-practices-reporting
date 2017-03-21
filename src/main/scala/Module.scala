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

import actors.ConfirmationActor
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import config._
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment, Logger}
import services._
import services.live.{CompaniesHouseAuth, CompaniesHouseSearch, NotifyServiceImpl}
import services.mocks.{MockCompanyAuth, MockCompanySearch, MockNotify}
import slicks.modules.DB

class Module(environment: Environment, configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {

    val appConfig = new AppConfig(configuration)

    appConfig.config.companiesHouse match {
      case Some(ch) =>
        bind(classOf[CompaniesHouseConfig]).toInstance(ch)
        bind(classOf[CompanySearchService]).to(classOf[CompaniesHouseSearch])
      case None =>
        Logger.debug("Wiring in Company Search Mock")
        bind(classOf[CompanySearchService]).to(classOf[MockCompanySearch])
    }

    appConfig.config.oAuth match {
      case Some(o) =>
        bind(classOf[OAuthConfig]).toInstance(o)
        bind(classOf[CompanyAuthService]).to(classOf[CompaniesHouseAuth])
      case None =>
        Logger.debug("Wiring in Company Auth Mock")
        bind(classOf[CompanyAuthService]).to(classOf[MockCompanyAuth])
    }

    appConfig.config.notifyService match {
      case Some(n) =>
        bind(classOf[NotifyConfig]).toInstance(n)
        bind(classOf[NotifyService]).to(classOf[NotifyServiceImpl])

      case None =>
        Logger.debug("Wiring in Notify Mock")
        bind(classOf[NotifyService]).to(classOf[MockNotify])
    }

    bind(classOf[Int])
      .annotatedWith(Names.named("session timeout"))
      .toInstance(appConfig.config.sessionTimeoutInMinutes.getOrElse(60))

    bind(classOf[GoogleAnalyticsConfig])
      .toInstance(appConfig.config.googleAnalytics.getOrElse(GoogleAnalyticsConfig.empty))

    bind(classOf[ServiceConfig])
      .toInstance(appConfig.config.service.getOrElse(ServiceConfig.empty))

    bind(classOf[DB]).asEagerSingleton()
    bindActor[ConfirmationActor]("confirmation-actor")

    bind(classOf[SessionCleaner]).asEagerSingleton()

  }
}

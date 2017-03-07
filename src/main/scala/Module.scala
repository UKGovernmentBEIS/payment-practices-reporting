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
import config.{AppConfig, MockConfig}
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment, Logger}
import services.companiesHouse.{CompaniesHouseAuth, CompaniesHouseSearch}
import services.mocks.{MockCompanyAuth, MockCompanySearch, MockNotify}
import services.notify.NotifyServiceImpl
import services.{CompanyAuthService, CompanySearchService, NotifyService, SessionCleaner}
import slicks.modules.DB

class Module(environment: Environment, configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {

    val mockConfig = new AppConfig(configuration).config.mockConfig.getOrElse(MockConfig.empty)

    val searchImpl = if (mockConfig.mockCompanySearch.getOrElse(false)) {
      Logger.debug("Wiring in Company Search Mock")
      classOf[MockCompanySearch]
    } else classOf[CompaniesHouseSearch]
    bind(classOf[CompanySearchService]).to(searchImpl)

    val authImpl = if (mockConfig.mockCompanyAuth.getOrElse(false)) {
      Logger.debug("Wiring in Company Auth Mock")
      classOf[MockCompanyAuth]
    } else classOf[CompaniesHouseAuth]
    bind(classOf[CompanyAuthService]).to(authImpl)

    val notifyImpl = if (mockConfig.mockNotify.getOrElse(false)) {
      Logger.debug("Wiring in Notify Mock")
      classOf[MockNotify]
    } else classOf[NotifyServiceImpl]
    bind(classOf[NotifyService]).to(notifyImpl)

    bind(classOf[DB]).asEagerSingleton()
    bindActor[ConfirmationActor]("confirmation-actor")

    bind(classOf[SessionCleaner]).asEagerSingleton()
  }
}

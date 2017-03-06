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
import config.AppConfig
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment}
import services.companiesHouse.CompaniesHouseSearch
import services.mocks.MockCompanySearch
import services.{CompanySearchService, SessionCleaner}
import slicks.modules.DB

class Module(environment: Environment, configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {

    val mockConfig = new AppConfig(configuration).config.mockConfig

    val searchImpl = if (mockConfig.mockCompanySearch) classOf[MockCompanySearch] else classOf[CompaniesHouseSearch]
    bind(classOf[CompanySearchService]).to(searchImpl)

    bind(classOf[DB]).asEagerSingleton()
    bindActor[ConfirmationActor]("confirmation-actor")

    bind(classOf[SessionCleaner]).asEagerSingleton()
  }
}

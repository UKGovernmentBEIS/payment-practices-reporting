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

package slicks

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slicks.modules.{ConfirmationModule, CoreModule, ReportModule, SessionModule}

class AllModules(val profile: JdbcProfile)
  extends CoreModule
    with ConfirmationModule
    with ReportModule
    with SessionModule {

  def this(dbConfig: DatabaseConfig[JdbcProfile]) =
    this(dbConfig.profile)

  @Inject()
  def this(dbConfigProvider: DatabaseConfigProvider) =
    this(dbConfigProvider.get[JdbcProfile])

  import profile.api._

  lazy val schema: profile.DDL =
    reportTable.schema ++
      contractDetailsTable.schema ++
      sessionTable.schema ++
      confirmationPendingTable.schema ++
      confirmationFailedTable.schema ++
      confirmationSentTable.schema
}

/*
 * Copyright (C) 2016  Department for Business, Energy and Industrial Strategy
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

import com.github.tminglei.slickpg.{ExPostgresDriver, PgDateSupportJoda, PgPlayJsonSupport}
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig

trait DBBinding extends ExPostgresDriver with PgDateSupportJoda {

  def dbConfigProvider: DatabaseConfigProvider

  lazy val dbConfig: DatabaseConfig[ExPostgresDriver] = dbConfigProvider.get[ExPostgresDriver]

  lazy val driver = new ExPostgresDriver with PgPlayJsonSupport with PgDateSupportJoda {
    override val pgjson = "jsonb"
  }

  lazy val db: driver.api.Database = dbConfig.db

  override val api = new API with DateTimeImplicits

  def schema: DDL = DDL(Seq(), Seq())
}

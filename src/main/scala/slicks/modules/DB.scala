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

package slicks.modules

import javax.inject.Inject

import com.github.tminglei.slickpg.{ExPostgresDriver, PgDateSupportJoda, PgPlayJsonSupport}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

class DB @Inject()(override val dbConfigProvider: DatabaseConfigProvider)
  extends CompanyModule
    with ReportModule
    with ExPostgresDriver
    with PgDateSupportJoda
    with PgPlayJsonSupport {

  Logger.info("DB created")

  override def pgjson: String = "jsonb"

  println("# --- !Ups")
  schema.createStatements.foreach(s=> println(s"$s;"))
  println
  println("# --- !Downs")
  schema.dropStatements.foreach(s=>println(s"$s;"))
}

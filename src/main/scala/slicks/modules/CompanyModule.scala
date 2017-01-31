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

import com.wellfactored.slickgen.IdType
import db.{CompanyId, CompanyRow}
import slicks.DBBinding

trait CompanyModule extends DBBinding {

  import api._

  implicit def CompanyIdMapper: BaseColumnType[CompanyId] = MappedColumnType.base[CompanyId, Long](_.id, CompanyId)

  type CompanyQuery = Query[CompanyTable, CompanyRow, Seq]

  class CompanyTable(tag: Tag) extends Table[CompanyRow](tag, "company") {
    def id = column[CompanyId]("id", O.Length(IdType.length), O.PrimaryKey, O.AutoInc)

    def companiesHouseIdentifier = column[String]("companies_house_identifier", O.Length(255))

    def companiesHouseIdentifierUniqueIdx = index("coho_id_unique_idx", companiesHouseIdentifier, unique = true)

    def name = column[String]("name", O.Length(255))

    def * = (id, companiesHouseIdentifier, name) <> (CompanyRow.tupled, CompanyRow.unapply)
  }

  lazy val companyTable = TableQuery[CompanyTable]

  override def schema = super.schema ++ companyTable.schema
}

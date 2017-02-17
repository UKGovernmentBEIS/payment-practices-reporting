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

import com.google.inject.ImplementedBy
import db.ReportRow
import forms.report.ReportFormModel
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.reactivestreams.Publisher

import scala.concurrent.Future

@ImplementedBy(classOf[ReportTable])
trait ReportRepo {
  def find(id: ReportId): Future[Option[ReportRow]]

  def reportFor(id: ReportId): Future[Option[CompanyReport]]

  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[ReportRow]]

  def list(cutoffDate: LocalDate, maxRows: Int = 100000): Publisher[CompanyReport]

  def save(confirmedBy: String, companiesHouseId: CompaniesHouseId, companyName: String, reportFormModel: ReportFormModel): Future[ReportId]
}

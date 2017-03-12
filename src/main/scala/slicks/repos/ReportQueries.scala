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

package slicks.repos

import slicks.DBBinding
import slicks.modules.ReportModule

trait ReportQueries {
  self: DBBinding with ReportModule =>

  import api._

  /**
    * Use left joins so that sections that are not completed come back as None
    */
  val reportQuery = {
    reportHeaderTable
      .joinLeft(reportPeriodTable).on(_.id === _.reportId)
      .joinLeft(paymentTermsTable).on(_._1.id === _.reportId)
      .joinLeft(paymentHistoryTable).on(_._1._1.id === _.reportId)
      .joinLeft(otherInfoTable).on(_._1._1._1.id === _.reportId)
      .joinLeft(filingTable).on(_._1._1._1._1.id === _.reportId)
      .map {
        case (((((header, period), terms), history), other), filing) => (header, period, terms, history, other, filing)
      }
  }

  val reportQueryC = Compiled(reportQuery)

  /**
    * Select reports that have been filed - i.e. all sections are present
    */
  val filedReportQuery = {
    for {
      header <- reportHeaderTable
      period <- reportPeriodTable if period.reportId === header.id
      terms <- paymentTermsTable if terms.reportId === header.id
      history <- paymentHistoryTable if history.reportId === header.id
      other <- otherInfoTable if other.reportId === header.id
      filing <- filingTable if filing.reportId === header.id
    } yield (header, period, terms, history, other, filing)
  }

  val filedReportQueryC = Compiled(filedReportQuery)
}

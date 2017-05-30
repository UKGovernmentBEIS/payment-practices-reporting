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

package services

import com.google.inject.ImplementedBy
import dbrows._
import forms.report.{ReportFormModel, ReportReviewModel, ReportingPeriodFormModel}
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.reactivestreams.Publisher
import slicks.repos.ReportTable

import scala.concurrent.Future

case class Report(
                   header: ReportHeaderRow,
                   period: Option[ReportPeriodRow],
                   paymentTerms: Option[PaymentTermsRow],
                   paymentHistory: Option[PaymentHistoryRow],
                   otherInfo: Option[OtherInfoRow],
                   paymentCodes: Option[PaymentCodesRow],
                   filing: Option[FilingRow]) {
  /**
    * If this report has been completed and filed then return Some `FiledReport`
    * otherwise None
    */
  def filed: Option[FiledReport] = for {
    p <- period
    terms <- paymentTerms
    hist <- paymentHistory
    other <- otherInfo
    codes <- paymentCodes
    f <- filing
  } yield FiledReport(header, p, terms, hist, other, codes, f)

  def isFiled: Boolean = filed.isDefined
}

case class FiledReport(
                        header: ReportHeaderRow,
                        period: ReportPeriodRow,
                        paymentTerms: PaymentTermsRow,
                        paymentHistory: PaymentHistoryRow,
                        otherInfo: OtherInfoRow,
                        paymentCodes: PaymentCodesRow,
                        filing: FilingRow
                      )

@ImplementedBy(classOf[ReportTable])
trait ReportService {
  def find(id: ReportId): Future[Option[Report]]

  def findFiled(id: ReportId): Future[Option[FiledReport]]

  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[Report]]

  def list(cutoffDate: LocalDate): Publisher[FiledReport]

  /**
    *
    * @param reportUrl - because we won't know what the `reportId` is until the table row is created,
    *                  and because we don't want to pass a Play request all the way down into this
    *                  module, the `reportUrl` parameter is a function that accepts a `reportId` and
    *                  generates the absolute url for it.
    */
  def create(
              confirmedBy: String,
              companiesHouseId: CompaniesHouseId,
              companyName: String,
              reportingPeriod: ReportingPeriodFormModel,
              reportFormModel: ReportFormModel,
              review: ReportReviewModel,
              confirmationEmailAddress: String,
              reportUrl: ReportId => String): Future[ReportId]
}

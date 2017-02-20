package slicks.modules

import slicks.DBBinding

trait ReportQueries {
  self : DBBinding with ReportModule =>

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
    reportHeaderTable
      .join(reportPeriodTable).on(_.id === _.reportId)
      .join(paymentTermsTable).on(_._1.id === _.reportId)
      .join(paymentHistoryTable).on(_._1._1.id === _.reportId)
      .join(otherInfoTable).on(_._1._1._1.id === _.reportId)
      .join(filingTable).on(_._1._1._1._1.id === _.reportId)
      .map {
        case (((((header, period), terms), history), other), filing) => (header, period, terms, history, other, filing)
      }
  }

  val filedReportQueryC = Compiled(filedReportQuery)
}

package forms.report

import javax.inject.Inject

import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.Mapping
import utils.TimeSource

import scala.util.Try

class Validations @Inject()(timeSource: TimeSource) {
  val companiesHouseId: Mapping[CompaniesHouseId] = nonEmptyText.transform(s => CompaniesHouseId(s), (c: CompaniesHouseId) => c.id)

  val percentageSplit: Mapping[PercentageSplit] = mapping(
    "percentWithin30Days" -> bigDecimal,
    "percentWithin60Days" -> bigDecimal,
    "percentBeyond60Days" -> bigDecimal
  )(PercentageSplit.apply)(PercentageSplit.unapply)
    .verifying("error.sumto100", sumTo100(_))

  private def sumTo100(ps: PercentageSplit): Boolean = (100.0 - ps.total).abs < 2.001

  val paymentHistory: Mapping[PaymentHistory] = mapping(
    "averageTimeToPay" -> number,
    "percentPaidWithinAgreedTerms" -> bigDecimal,
    "percentageSplit" -> percentageSplit
  )(PaymentHistory.apply)(PaymentHistory.unapply)

  val paymentTerms: Mapping[PaymentTerms] = mapping(
    "terms" -> nonEmptyText,
    "maximumContractPeriod" -> nonEmptyText,
    "paymentTermsChanged" -> boolean,
    "paymentTermsChangedComment" -> optional(nonEmptyText),
    "paymentTermsNotified" -> boolean,
    "paymentTermsNotifiedComment" -> optional(nonEmptyText),
    "paymentTermsComment" -> optional(nonEmptyText)
  )(PaymentTerms.apply)(PaymentTerms.unapply)
    .verifying("error.changedcomment.required", pt => pt.paymentTermsChanged && pt.paymentTermsChangedComment.isDefined)
    .verifying("error.notifiedcomment.required", pt => pt.paymentTermsChanged && pt.paymentTermsChangedNotified && pt.paymentTermsChangedNotifiedComment.isDefined)

  def validateTermsChanged(paymentTerms: PaymentTerms): Boolean = {
    (paymentTerms.paymentTermsChanged, paymentTerms.paymentTermsChangedComment) match {
      case (true, Some(_)) => true
      case (false, None) => true
      case _ => false
    }
  }

  val dateFields: Mapping[DateFields] = mapping(
    "day" -> number,
    "month" -> number,
    "year" -> number
  )(DateFields.apply)(DateFields.unapply)
    .verifying("error.date", fields => validateFields(fields))

  val dateFromFields: Mapping[LocalDate] = dateFields.transform(toDate, fromDate)

  private def validateFields(fields: DateFields): Boolean = Try(toDate(fields)).isSuccess

  /**
    * Warning: Will throw an exception if the fields don't constitute a valid date. This is provided
    * to support the `.transform` call below on the basis that the fields themselves will have already
    * been verified with `validateFields`
    */
  private def toDate(fields: DateFields): LocalDate = new LocalDate(fields.year, fields.month, fields.day)

  private def fromDate(date: LocalDate): DateFields = DateFields(date.getDayOfMonth, date.getMonthOfYear, date.getYear)

  val dateRange: Mapping[DateRange] = mapping(
    "startDate" -> dateFromFields,
    "endDate" -> dateFromFields
  )(DateRange.apply)(DateRange.unapply)
    .verifying("error.beforenow", dr => dr.startDate.isBefore(now()))
    .verifying("error.endafterstart", dr => dr.endDate.isAfter(dr.startDate))

  private def now() = new LocalDate(timeSource.currentTimeMillis())

  val reportFormModel = mapping(
    "companiesHouseId" -> companiesHouseId,
    "reportDates" -> dateRange,
    "paymentHistory" -> paymentHistory,
    "paymentTerms" -> paymentTerms,
    "disputeResolution" -> nonEmptyText,
    "hasPaymentCodes" -> boolean,
    "paymentCodes" -> optional(nonEmptyText),
    "offerEInvoicing" -> boolean,
    "offerSupplyChainFinancing" -> boolean,
    "retentionChargesInPolicy" -> boolean,
    "retentionChargesInPast" -> boolean
  )(ReportFormModel.apply)(ReportFormModel.unapply)
    .verifying("error.paymentcodes.required", rf => rf.hasPaymentCodes && rf.paymentCodes.isDefined)
}

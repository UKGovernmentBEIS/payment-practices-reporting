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

package forms.report

import javax.inject.Inject

import config.ServiceConfig
import forms.DateRange
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalactic.TripleEquals._
import play.api.Logger
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, FormError, Mapping}
import utils.YesNo.{No, Yes}
import utils.{AdjustErrors, TimeSource}

class Validations @Inject()(timeSource: TimeSource, serviceConfig: ServiceConfig) {

  import ConditionalTextValidations._
  import PaymentTermsChangedValidations._
  import ReportConstants._
  import forms.Validations._

  def isBlank(s: String): Boolean = s.trim() === ""

  val percentage = number.verifying("error.percentage", n => n >= 0 && n <= 100)

  private val sumTo100 = (ps: PercentageSplit) => (100 - ps.total).abs <= 2

  val percentageSplit: Mapping[PercentageSplit] = mapping(
    "percentWithin30Days" -> percentage,
    "percentWithin60Days" -> percentage,
    "percentBeyond60Days" -> percentage
  )(PercentageSplit.apply)(PercentageSplit.unapply)
    .verifying("error.sumto100", sumTo100)

  val paymentHistory: Mapping[PaymentHistory] = mapping(
    "averageDaysToPay" -> number(min = 0),
    "percentPaidBeyondAgreedTerms" -> percentage,
    "percentageSplit" -> percentageSplit
  )(PaymentHistory.apply)(PaymentHistory.unapply)


  private val pt: Mapping[PaymentTerms] = mapping(
    "shortestPaymentPeriod" -> number(min = 0),
    "longestPaymentPeriod" -> optional(number(min = 0)),
    "terms" -> words(1, paymentTermsWordCount),
    "maximumContractPeriod" -> number(min = 0),
    "maximumContractPeriodComment" -> optional(words(1, maxContractPeriodCommentWordCount)),
    "paymentTermsChanged" -> paymentTermsChanged,
    "paymentTermsComment" -> optional(words(1, paymentTermsCommentWordCount)),
    "disputeResolution" -> words(1, disputeResolutionWordCount)
  )(PaymentTerms.apply)(PaymentTerms.unapply)
    .verifying("error.shortestNotLessThanLongest", pt => pt.longestPaymentPeriod.forall(longest => pt.shortestPaymentPeriod < longest))

  val paymentTerms: Mapping[PaymentTerms] = AdjustErrors(pt) { (key, errs) =>
    errs.map {
      case FormError(k, messages, args) if messages.headOption.contains("error.shortestNotLessThanLongest") =>
        FormError(s"paymentTerms.longestPaymentPeriod", messages, args)

      case e => e
    }
  }

  private def now() = new LocalDate(timeSource.currentTimeMillis())

  /**
    * The service does not go live until April 6 2017 so we should not accept period end
    * dates that are prior to that date. In order to support testing in non-live environments
    * I've provided a config parameter to allow the date to be set to something different.
    */
  private val serviceStartDate = serviceConfig.startDate.getOrElse(ServiceConfig.defaultServiceStartDate)
  private val df = DateTimeFormat.forPattern("d MMMM yyyy")
  private val serviceStartConstraint = Constraint { dr: DateRange =>
    if (dr.endDate.isBefore(serviceStartDate)) {
      val invalid = Invalid("error.beforeservicestart", df.print(serviceStartDate))
      Logger.debug(invalid.toString)
      invalid
    }
    else Valid
  }

  private val reportDates =
    dateRange
      .verifying("error.notfuture", dr => !now().isBefore(dr.endDate))
      .verifying(serviceStartConstraint)

  val reportingPeriodFormModel = mapping(
    "reportDates" -> reportDates,
    "hasQualifyingContracts" -> yesNo
  )(ReportingPeriodFormModel.apply)(ReportingPeriodFormModel.unapply)

  private val paymentCodesValidation = "paymentCodes" -> conditionalText(paymentCodesWordCount)

  val shortFormModel = mapping(
    paymentCodesValidation
  )(ShortFormModel.apply)(ShortFormModel.unapply)

  val reportFormModel = mapping(
    "paymentHistory" -> paymentHistory,
    "paymentTerms" -> paymentTerms,
    "offerEInvoicing" -> yesNo,
    "offerSupplyChainFinancing" -> yesNo,
    "retentionChargesInPolicy" -> yesNo,
    "retentionChargesInPast" -> yesNo,
    paymentCodesValidation
  )(LongFormModel.apply)(LongFormModel.unapply)

  val reportReviewModel = mapping(
    "confirmedBy" -> nonEmptyText(maxLength = 255),
    "confirmed" -> checked("error.confirm")
  )(ReportReviewModel.apply)(ReportReviewModel.unapply)

  val emptyReportingPeriod: Form[ReportingPeriodFormModel] = Form(reportingPeriodFormModel)
  val emptyLongForm: Form[LongFormModel] = Form(reportFormModel)
  val emptyShortForm: Form[ShortFormModel] = Form(shortFormModel)
  val emptyReview: Form[ReportReviewModel] = Form(reportReviewModel)
}


/**
  * The PaymentTermsChanged handling is quite complex, with an interaction between two ConditionalTexts.
  * These mappings capture that interaction. If the first question (`Changed`) is answered `No` then the
  * second question (`Notified`) need not be answered, and if it is then any answer will be discarded.
  *
  * If `Changed` is answered `Yes` then the `Notified` question must be answered, with the usual ConditionalText
  * constraints applied.
  *
  * There is also a need to adjust various errors from structure-level validations so that they are associated
  * with the relevant sub-field.
  */
object PaymentTermsChangedValidations {

  import ConditionalTextValidations._
  import ReportConstants._

  private val errorMustAnswer = "error.mustanswer"

  private val errorNotifiedTextRequired = "error.notified.text.required"

  private val answerNotifiedIfChanged = Constraint { ch: PaymentTermsChanged =>
    ch match {
      case PaymentTermsChanged(ConditionalText(Yes, _), None) => Invalid(errorMustAnswer)
      case PaymentTermsChanged(ConditionalText(Yes, _), Some(ConditionalText(Yes, None))) => Invalid(errorNotifiedTextRequired)
      case PaymentTermsChanged(ConditionalText(No, _), _) => Valid
      case _ => Valid
    }
  }

  private val ptc = mapping(
    "changed" -> conditionalText(paymentTermsChangedWordCount),
    "notified" -> optional(yesNoText(paymentTermsNotifiedWordCount))
  )(PaymentTermsChanged.apply)(PaymentTermsChanged.unapply)
    .transform(_.normalise, (ptc: PaymentTermsChanged) => ptc)
    .verifying(answerNotifiedIfChanged)

  val paymentTermsChanged = AdjustErrors(ptc) { (key, errs) =>
    def keyFor(baseKey: String, subKey: String) = if (baseKey === "") subKey else s"$baseKey.$subKey"

    errs.map {
      case FormError(k, messages, args) if messages.headOption.contains(errorMustAnswer) =>
        FormError(keyFor(k, "notified.yesNo"), messages, args)

      case FormError(k, messages, args) if messages.headOption.contains(errorNotifiedTextRequired) =>
        FormError(keyFor(k, "notified.text"), Seq(errorRequired), args)

      case FormError(k, messages, args) if k === keyFor(key, "notified") =>
        FormError(keyFor(k, "text"), messages, args)

      case e => e
    }
  }
}

object ConditionalTextValidations {

  import forms.Validations._

  val errorRequired = "error.required"

  /**
    * A yesNoText mapping combines a yesNo field with an optional text field to produce a ConditionalText
    * output. The output is normalised so that if the yesNo is answered `No` then any value for the text
    * field is discarded. No further validations are applied (so there is no check that the text is
    * supplied when the yesNo is `Yes` - see `conditionalText` for that)
    */
  def yesNoText(maxWords: Int) = mapping(
    "yesNo" -> yesNo,
    "text" -> optional(text)
  )(ConditionalText.apply)(ConditionalText.unapply)
    .transform(_.normalize, (ct: ConditionalText) => ct)

  /**
    * Validate a ConditionalText to check that if the yesNo is `Yes` then the text is supplied.
    */
  private val textRequiredIfYes = Constraint { ct: ConditionalText =>
    ct match {
      case ConditionalText(Yes, None) => Invalid(errorRequired)
      case _ => Valid
    }
  }

  private def condText(maxWords: Int): Mapping[ConditionalText] = yesNoText(maxWords).verifying(textRequiredIfYes)

  /**
    * `conditionalText` enhances a `yesNoText` with validation that the text is present when
    * the yesNo is `Yes`.
    *
    * Move any messages attached to the base key to the `text` sub-key. The
    * only message we're expecting is the `error.required` generated by the
    * `textRequiredIfYes` constraint.
    */
  def conditionalText(maxWords: Int) = AdjustErrors(condText(maxWords)) { (key, errs) =>
    errs.map {
      case FormError(k, messages, args) if k === key => FormError(s"$k.text", messages, args)
      case e => e
    }
  }
}
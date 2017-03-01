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

import models.CompaniesHouseId
import org.joda.time.LocalDate
import org.scalactic.TripleEquals._
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{FormError, Mapping}
import utils.YesNo.{No, Yes}
import utils.{AdjustErrors, TimeSource}

class Validations @Inject()(timeSource: TimeSource) {

  import ConditionalTextValidations._
  import PaymentTermsChangedValidations._
  import forms.Validations._

  def isBlank(s: String): Boolean = s.trim() === ""

  val companiesHouseId: Mapping[CompaniesHouseId] = nonEmptyText.transform(s => CompaniesHouseId(s), (c: CompaniesHouseId) => c.id)

  val percentage = number(min = 0, max = 100)

  val percentageSplit: Mapping[PercentageSplit] = mapping(
    "percentWithin30Days" -> percentage,
    "percentWithin60Days" -> percentage,
    "percentBeyond60Days" -> percentage
  )(PercentageSplit.apply)(PercentageSplit.unapply)
    .verifying("error.sumto100", sumTo100(_))

  private def sumTo100(ps: PercentageSplit): Boolean = (100 - ps.total).abs <= 2

  val paymentHistory: Mapping[PaymentHistory] = mapping(
    "averageDaysToPay" -> number(min = 0),
    "percentPaidBeyondAgreedTerms" -> percentage,
    "percentageSplit" -> percentageSplit
  )(PaymentHistory.apply)(PaymentHistory.unapply)

  val paymentTerms: Mapping[PaymentTerms] = mapping(
    "terms" -> nonEmptyText,
    "paymentPeriod" -> number(min = 0),
    "maximumContractPeriod" -> number(min = 0),
    "maximumContractPeriodComment" -> optional(nonEmptyText),
    "paymentTermsChanged" -> paymentTermsChanged,
    "paymentTermsComment" -> optional(nonEmptyText),
    "disputeResolution" -> nonEmptyText
  )(PaymentTerms.apply)(PaymentTerms.unapply)

  private def now() = new LocalDate(timeSource.currentTimeMillis())

  val reportFormModel = mapping(
    "reportDates" -> dateRange.verifying("error.beforenow", dr => dr.startDate.isBefore(now())),
    "paymentHistory" -> paymentHistory,
    "paymentTerms" -> paymentTerms,
    "paymentCodes" -> conditionalText,
    "offerEInvoicing" -> yesNo,
    "offerSupplyChainFinancing" -> yesNo,
    "retentionChargesInPolicy" -> yesNo,
    "retentionChargesInPast" -> yesNo
  )(ReportFormModel.apply)(ReportFormModel.unapply)

  val reportReviewModel = mapping(
    "confirmed" -> boolean,
    "confirmedBy" -> nonEmptyText
  )(ReportReviewModel.apply)(ReportReviewModel.unapply)
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
    "changed" -> conditionalText,
    "notified" -> optional(yesNoText)
  )(PaymentTermsChanged.apply)(PaymentTermsChanged.unapply)
    .transform(_.normalise, (ptc: PaymentTermsChanged) => ptc)
    .verifying(answerNotifiedIfChanged)

  val paymentTermsChanged = AdjustErrors(ptc) { (key, errs) =>
    def keyFor(baseKey: String, subKey: String) = if (baseKey === "") subKey else s"$baseKey.$subKey"

    errs.map {
      case FormError(k, messages, args) if messages.headOption.contains(errorMustAnswer) => FormError(keyFor(k, "notified.yesNo"), messages, args)
      case FormError(k, messages, args) if messages.headOption.contains(errorNotifiedTextRequired) =>

        FormError(keyFor(k, "notified.text"), Seq(errorRequired), args)
      case FormError(k, messages, args) if k === keyFor(key, "notified") => FormError(keyFor(k, "text"), messages, args)
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
  val yesNoText = mapping(
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

  private val condText: Mapping[ConditionalText] = yesNoText.verifying(textRequiredIfYes)

  /**
    * `conditionalText` enhances a `yesNoText` with validation that the text is present when
    * the yesNo is `Yes`.
    *
    * Move any messages attached to the base key to the `text` sub-key. The
    * only message we're expecting is the `error.required` generated by the
    * `textRequiredIfYes` constraint.
    */
  val conditionalText = AdjustErrors(condText) { (key, errs) =>
    errs.map {
      case FormError(k, messages, args) if k === key => FormError(s"$k.text", messages, args)
      case e => e
    }
  }
}
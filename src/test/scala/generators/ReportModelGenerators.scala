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

package generators

import forms.DateRange
import forms.report._
import models.CompaniesHouseId
import org.joda.time.LocalDate
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.{alphaLowerChar, alphaUpperChar, frequency}
import org.scalacheck.{Arbitrary, Gen}
import services.CompanyDetail
import utils.YesNo
import utils.YesNo._

import scala.language.implicitConversions

object ReportModelGenerators {

  import ReportConstants._

  def alphaSpaceChar: Gen[Char] = frequency((1, alphaUpperChar), (9, alphaLowerChar), (2, Gen.const(' ')))

  def alphaSpaceString: Gen[String] = Gen.listOf(alphaSpaceChar).map(_.mkString)

  def genText(maxLength: Int): Gen[String] = alphaSpaceString.map(_.take(maxLength))

  val genCompanyName: Gen[String] = genText(255)

  val genCompaniesHouseId: Gen[CompaniesHouseId] = Gen.alphaStr.map(_.take(255)).map(CompaniesHouseId)

  val genCompanyDetails:Gen[CompanyDetail] = for {
    name <- genCompanyName
    id <- genCompaniesHouseId
  } yield CompanyDetail(id, name)

  def genConditionalText(maxLength: Int): Gen[ConditionalText] =
    Gen.option(genText(maxLength)).map(ConditionalText.apply)

  val genYesNo: Gen[YesNo with Product with Serializable] = Gen.oneOf(Yes, No)
  implicit val arbYesNo: Arbitrary[YesNo] = Arbitrary(genYesNo)

  val earliestDateMS: Long = new LocalDate(2017, 4, 1).toDate.getTime

  private val thirtyDays: Long = 1000L * 60 * 60 * 24 * 30
  private val oneYear   : Long = 1000L * 60 * 60 * 24 * 365

  val genDateRange: Gen[DateRange] = for {
    startMS <- Gen.choose(earliestDateMS, earliestDateMS + oneYear)
    endMS <- Gen.choose(thirtyDays, oneYear).map(_ + startMS)
  } yield DateRange(new LocalDate(startMS), new LocalDate(endMS))

  implicit val arbDateRange: Arbitrary[DateRange] = Arbitrary(genDateRange)

  val genPercentageSplit: Gen[PercentageSplit] = for {
    p1 <- Gen.chooseNum(0, 100)
    p2 <- Gen.chooseNum(p1, 100).map(_ - p1)
    variation <- Gen.chooseNum(-1, 1)
    p3 = 0.max(100 - (p1 + p2) + variation)
  } yield {
    PercentageSplit(p1, p2, p3)
  }

  val genDisputeResolution: Gen[DisputeResolution] =
    genText(disputeResolutionCharCount).map(DisputeResolution)

  val genPaymentStatistics: Gen[PaymentStatistics] = for {
    days <- Gen.chooseNum(0, 90)
    split <- arbitrary[PercentageSplit]
    paidLate <- Gen.chooseNum(0, 100)
  } yield {
    PaymentStatistics(days, split, paidLate)
  }

  val genPaymentTermsChanged: Gen[PaymentTermsChanged] = for {
    comment <- genConditionalText(paymentTermsChangedCharCount)
    notified <- Gen.option(genConditionalText(paymentTermsNotifiedCharCount))
  } yield {
    PaymentTermsChanged(comment, notified)
  }

  val genPaymentTerms: Gen[PaymentTerms] = for {
    shortest <- Gen.chooseNum(0, 50)
    longest <- Gen.option(Gen.chooseNum(shortest, 80))
    terms <- genText(paymentTermsCharCount)
    maxPeriod <- Gen.chooseNum(0, 90)
    maxComment <- Gen.option(genText(maxContractPeriodCommentCharCount))
    changed <- genPaymentTermsChanged
    comment <- Gen.option(genText(paymentTermsCommentCharCount))
  } yield PaymentTerms(shortest, longest, terms, maxPeriod, maxComment, changed, comment)

  val genOtherInformation: Gen[OtherInformation] = for {
    ei <- arbitrary[YesNo]
    scf <- arbitrary[YesNo]
    inPolicy <- arbitrary[YesNo]
    inPast <- arbitrary[YesNo]
    pc <- genConditionalText(paymentCodesCharCount)
  } yield {
    OtherInformation(ei, scf, inPolicy, inPast, pc)
  }

  implicit val arbPercentageSplit    : Arbitrary[PercentageSplit]     = Arbitrary(genPercentageSplit)
  implicit val arbDisputeResolution  : Arbitrary[DisputeResolution]   = Arbitrary(genDisputeResolution)
  implicit val arbPaymentStatistics  : Arbitrary[PaymentStatistics]   = Arbitrary(genPaymentStatistics)
  implicit val arbPaymentTermsChanged: Arbitrary[PaymentTermsChanged] = Arbitrary(genPaymentTermsChanged)
  implicit val arbPaymentTerms       : Arbitrary[PaymentTerms]        = Arbitrary(genPaymentTerms)
  implicit val arbOtherInformation   : Arbitrary[OtherInformation]    = Arbitrary(genOtherInformation)

  val genLongFormModel: Gen[LongFormModel] = for {
    ps <- arbitrary[PaymentStatistics]
    pt <- arbitrary[PaymentTerms]
    dr <- arbitrary[DisputeResolution]
    oi <- arbitrary[OtherInformation]
  } yield LongFormModel(ps, pt, dr, oi)

  val genShortFormModel: Gen[ShortFormModel] =
    genConditionalText(paymentCodesCharCount).map(ShortFormModel)

  val genReportingPeriodFormModel: Gen[ReportingPeriodFormModel] = for {
    dateRange <- arbitrary[DateRange]
    hasQC <- arbitrary[YesNo]
  } yield ReportingPeriodFormModel(dateRange, hasQC)

  val genReportReviewModel: Gen[ReportReviewModel] = for {
    confirmedBy <- genText(50)
    confirmed <- arbitrary[Boolean]
  } yield ReportReviewModel(confirmedBy, confirmed)

  implicit val arbLongFormModel           : Arbitrary[LongFormModel]            = Arbitrary(genLongFormModel)
  implicit val arbShortFormModel          : Arbitrary[ShortFormModel]           = Arbitrary(genShortFormModel)
  implicit val arbReportingPeriodFormModel: Arbitrary[ReportingPeriodFormModel] = Arbitrary(genReportingPeriodFormModel)
  implicit val arbReportReviewFormModel   : Arbitrary[ReportReviewModel]        = Arbitrary(genReportReviewModel)

  def main(args: Array[String]): Unit = {
    arbitrary[ReportingPeriodFormModel].sample.foreach(println)
  }
}

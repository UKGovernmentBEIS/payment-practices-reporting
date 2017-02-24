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

package controllers

import javax.inject.Inject

import calculator.Calculator
import db._
import forms.DateRange
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import play.twirl.api.Html
import questionnaire._
import slicks.modules.FiledReport
import utils.YesNo

class VisualTestController @Inject()(questions: Questions, summarizer: Summarizer)(implicit messages: MessagesApi) extends Controller with PageHelper {

  import questions._

  val startDate = new LocalDate(2017, 1, 1)
  val endDate = new LocalDate(2017, 12, 31)

  def show = Action { implicit request =>
    val index = views.html.index()
    val download = views.html.download.accessData()
    val qStart = views.html.questionnaire.start()
    val reasons = None +: Seq("reason.firstyear", "reason.company.notlargeenough", "reason.group.notlargeenough").map(Some(_))
    val exempts = reasons.map(views.html.questionnaire.exempt(_))
    val requireds =
      Seq(views.html.questionnaire.required(summarizer.summarize(DecisionState.secondYear)),
        views.html.questionnaire.required(summarizer.summarize(DecisionState.thirdYear)))
    val calcs = Seq(
      views.html.calculator.calculator(CalculatorController.emptyForm),
      views.html.calculator.calculator(CalculatorController.emptyForm.bind(Map[String, String]())))


    val calc = Calculator(calculator.FinancialYear(DateRange(startDate, endDate)))
    val answers = Seq(views.html.calculator.answer(false, calc, CalculatorController.df))
    val searches = Seq(views.html.search.search("", None, Map(), "", _ => "", _ => ""))

    val reports = Seq(
      views.html.search.report(healthyReport, CalculatorController.df)
    )

    val content = (
      Seq(index, download, qStart)
        ++ questionPages
        ++ exempts
        ++ requireds
        ++ calcs
        ++ answers
        ++ searches
        ++ reports
      ).zipWithIndex.flatMap { case (x, i) => Seq(Html(s"<hr/>screen ${i + 1}"), x) }
    Ok(page(content: _*))
  }

  val questionPages = Seq(
    isCompanyOrLLPQuestion,
    financialYearQuestion,
    hasSubsidiariesQuestion,
    companyTurnoverQuestionY2,
    companyBalanceSheetQuestionY2,
    companyEmployeesQuestionY2,
    companyTurnoverQuestionY3,
    companyBalanceSheetQuestionY3,
    companyEmployeesQuestionY3,
    subsidiaryTurnoverQuestionY2,
    subsidiaryBalanceSheetQuestionY2,
    subsidiaryEmployeesQuestionY2,
    subsidiaryTurnoverQuestionY3,
    subsidiaryBalanceSheetQuestionY3,
    subsidiaryEmployeesQuestionY3
  ).map(views.html.questionnaire.question("", _))

  val states = Seq(
    StateSummary(None, ThresholdSummary(None, None, None), ThresholdSummary(None, None, None))
  )

  import YesNo._

  val reportId = ReportId(0)

  val healthyReport = FiledReport(
    ReportHeaderRow(reportId, "ABC Limited", CompaniesHouseId("1234567890"), LocalDate.now, LocalDate.now),
    ReportPeriodRow(reportId, startDate, endDate),
    PaymentTermsRow(reportId, "payment terms", 30, 30, Some("Maximum period is very fair"), Some("Payment terms have changed"), Some("We told everyone"), Some("Other comments"), "Dispute resolution process is the best"),
    PaymentHistoryRow(reportId, 30, 10, 33, 33, 33),
    OtherInfoRow(reportId, No, Yes, No, Yes, Some("Payment Practice Code")),
    FilingRow(reportId, LocalDate.now, "The big boss")
  )

}

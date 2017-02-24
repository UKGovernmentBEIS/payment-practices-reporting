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
import forms.DateRange
import org.joda.time.LocalDate
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import play.twirl.api.Html
import questionnaire._

class VisualTestController @Inject()(questions: Questions, summarizer: Summarizer)(implicit messages: MessagesApi) extends Controller with PageHelper {

  import questions._

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

    val calc = Calculator(calculator.FinancialYear(DateRange(new LocalDate(2017, 1, 1), new LocalDate(2017, 12, 31))))
    val answers = Seq(views.html.calculator.answer(false, calc, CalculatorController.df))

    //val searches = Seq(views.html.search.search("", None, Map()))

    val content = (
      Seq(index, download, qStart)
        ++ questionPages
        ++ exempts
        ++ requireds
        ++ calcs
        ++ answers
      ).zipWithIndex.flatMap{case (x, i) => Seq(Html(s"<hr/>screen ${i+1}"), x )}
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

}

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

package questionnaire

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2}
import org.scalatest.{EitherValues, Matchers, WordSpecLike}
import questionnaire.FinancialYear.{Second, ThirdOrLater}
import utils.YesNo
import utils.YesNo.{No, Yes}

class DecisionTreeTest extends WordSpecLike with Matchers with EitherValues with TableDrivenPropertyChecks {

  import questionnaire.DecisionTree._
  import questionnaire.Questions._

  "empty answers should return first question" in {
    val result = checkAnswers(Seq()).right.value
    result shouldBe a[YesNoNode]
    result.asInstanceOf[YesNoNode].question shouldBe isCompanyOrLLPQuestion
  }

  "wrong answer should return error" in {
    val answers = Seq(YesNoAnswer(hasSubsidiariesQuestion.id, Yes))

    val result = checkAnswers(answers)
    result shouldBe a[Left[String, _]]
    result.left.value shouldBe "Answer YesNoAnswer(3,Yes) did not match question 1"
  }

  "check questions for year 2" in {
    val table: TableFor2[Seq[Answer], YesNoQuestion] = Table(("answers", "expected question"), FYData.expectedQuestions(Second): _*)
    forAll(table) { case (as, q) =>
      val result = checkAnswers(as)
      result shouldBe a[Right[_, _]]
      result.right.value shouldBe a[YesNoNode]
      result.right.value.asInstanceOf[YesNoNode].question.textKey shouldBe q.textKey
    }
  }

  "check decisions for year 2" in {
    val table: TableFor2[Seq[Answer], Decision] = Table(("answers", "expected question"), FYData.expectedDecisions(Second): _*)
    forAll(table) { case (as, d) =>
      val result = checkAnswers(as)
      result shouldBe a[Right[_, _]]
      result.right.value shouldBe a[DecisionNode]
      result.right.value.asInstanceOf[DecisionNode].decision shouldBe d
    }
  }

  "check questions for year 3" in {
    val table: TableFor2[Seq[Answer], YesNoQuestion] = Table(("answers", "expected question"), FYData.expectedQuestions(ThirdOrLater): _*)
    forAll(table) { case (as, q) =>
      val result = checkAnswers(as)
      result shouldBe a[Right[_, _]]
      result.right.value shouldBe a[YesNoNode]
      result.right.value.asInstanceOf[YesNoNode].question.textKey shouldBe q.textKey
    }
  }

  "check decisions for year 3" in {
    val table: TableFor2[Seq[Answer], Decision] = Table(("answers", "expected question"), FYData.expectedDecisions(ThirdOrLater): _*)
    forAll(table) { case (as, d) =>
      val result = checkAnswers(as)
      result shouldBe a[Right[_, _]]
      result.right.value shouldBe a[DecisionNode]
      result.right.value.asInstanceOf[DecisionNode].decision shouldBe d
    }
  }

}

object FYData {

  import Questions._

  def expectedQuestions(financialYear: FinancialYear): Seq[(Seq[Answer], YesNoQuestion)] = financialYear match {
    case Second       => expectedQuestionsY2
    case ThirdOrLater => expectedQuestionsY3
    case _            => ???
  }

  def expectedDecisions(financialYear: FinancialYear): Seq[(Seq[Answer], Decision)] = financialYear match {
    case Second       => expectedDecisionsY2
    case ThirdOrLater => expectedDecisionsY3
    case _            => ???
  }

  val companyY2    = Seq(companyTurnoverQuestionY2, companyBalanceSheetQuestionY2, companyEmployeesQuestionY2)
  val subsidiaryY2 = Seq(subsidiaryTurnoverQuestionY2, subsidiaryBalanceSheetQuestionY2, subsidiaryEmployeesQuestionY2)
  val companyY3    = Seq(companyTurnoverQuestionY3, companyBalanceSheetQuestionY3, companyEmployeesQuestionY3)
  val subsidiaryY3 = Seq(subsidiaryTurnoverQuestionY3, subsidiaryBalanceSheetQuestionY3, subsidiaryEmployeesQuestionY3)

  def answer(q: YesNoQuestion, a: YesNo): YesNoAnswer = YesNoAnswer(q.id, a)

  def answers(qs: Seq[YesNoQuestion], as: Seq[YesNo]): Seq[YesNoAnswer] = qs.zip(as).map { case (q, a) => answer(q, a) }

  private val y2startState = Seq(answer(isCompanyOrLLPQuestion, Yes), FinancialYearAnswer(financialYearQuestion.id, Second))


  private val y2Y   : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY2, Seq(Yes)), companyBalanceSheetQuestionY2)
  private val y2YY  : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY2, Seq(Yes, Yes)), hasSubsidiariesQuestion)
  private val y2YN  : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY2, Seq(Yes, No)), companyEmployeesQuestionY2)
  private val y2YNY : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY2, Seq(Yes, No, Yes)), hasSubsidiariesQuestion)
  private val y2N   : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY2, Seq(No)), companyBalanceSheetQuestionY2)
  private val y2NY  : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY2, Seq(No, Yes)), companyEmployeesQuestionY2)
  private val y2NN  : (Seq[YesNoAnswer], Decision)      = (answers(companyY2, Seq(No, No)), Exempt("reason.company.notlargeenough"))
  private val y2NYY : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY2, Seq(No, Yes, Yes)), hasSubsidiariesQuestion)
  private val y2YYn : (Seq[YesNoAnswer], Decision)      = (y2YY._1 :+ answer(hasSubsidiariesQuestion, No), Required)
  private val y2YYy : (Seq[YesNoAnswer], YesNoQuestion) = (y2YY._1 :+ answer(hasSubsidiariesQuestion, Yes), subsidiaryTurnoverQuestionY2)
  private val y2YYyY: (Seq[YesNoAnswer], YesNoQuestion) = (y2YYy._1 ++ answers(subsidiaryY2, Seq(Yes)), subsidiaryBalanceSheetQuestionY2)

  private val y2YYyYY : (Seq[YesNoAnswer], Decision)      = (y2YYy._1 ++ answers(subsidiaryY2, Seq(Yes, Yes)), Required)
  private val y2YYyYN : (Seq[YesNoAnswer], YesNoQuestion) = (y2YYy._1 ++ answers(subsidiaryY2, Seq(Yes, No)), subsidiaryEmployeesQuestionY2)
  private val y2YYyYNY: (Seq[YesNoAnswer], Decision)      = (y2YYy._1 ++ answers(subsidiaryY2, Seq(Yes, No, Yes)), Required)
  private val y2YYyYNN: (Seq[YesNoAnswer], Decision)      = (y2YYy._1 ++ answers(subsidiaryY2, Seq(Yes, No, No)), Exempt("reason.group.notlargeenough"))
  private val y2YYyN  : (Seq[YesNoAnswer], YesNoQuestion) = (y2YYy._1 ++ answers(subsidiaryY2, Seq(No)), subsidiaryBalanceSheetQuestionY2)
  private val y2YYyNY : (Seq[YesNoAnswer], YesNoQuestion) = (y2YYy._1 ++ answers(subsidiaryY2, Seq(No, Yes)), subsidiaryEmployeesQuestionY2)
  private val y2YYyNYY: (Seq[YesNoAnswer], Decision)      = (y2YYy._1 ++ answers(subsidiaryY2, Seq(No, Yes, Yes)), Required)
  private val y2YYyNYN: (Seq[YesNoAnswer], Decision)      = (y2YYy._1 ++ answers(subsidiaryY2, Seq(No, Yes, No)), Exempt("reason.group.notlargeenough"))
  private val y2YYyNN : (Seq[YesNoAnswer], Decision)      = (y2YYy._1 ++ answers(subsidiaryY2, Seq(No, No)), Exempt("reason.group.notlargeenough"))

  private val expectedQuestionsY2: Seq[(Seq[Answer], YesNoQuestion)] =
    Seq(y2Y, y2YY, y2YN, y2YNY, y2N, y2NY, y2NYY, y2YYy, y2YYyY, y2YYyYN, y2YYyN, y2YYyNY).map { case (as, q) =>
      (y2startState ++ as, q)
    }
  private val expectedDecisionsY2: Seq[(Seq[Answer], Decision)]      =
    Seq(y2NN, y2YYn, y2YYyYY, y2YYyYNY, y2YYyYNN, y2YYyNYY, y2YYyNYN, y2YYyNN).map { case (as, d) =>
      (y2startState ++ as, d)
    }

  private val y3startState = Seq(answer(isCompanyOrLLPQuestion, Yes), FinancialYearAnswer(financialYearQuestion.id, ThirdOrLater))

  private val y3Y   : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY3, Seq(Yes)), companyBalanceSheetQuestionY3)
  private val y3YY  : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY3, Seq(Yes, Yes)), hasSubsidiariesQuestion)
  private val y3YN  : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY3, Seq(Yes, No)), companyEmployeesQuestionY3)
  private val y3YNY : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY3, Seq(Yes, No, Yes)), hasSubsidiariesQuestion)
  private val y3N   : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY3, Seq(No)), companyBalanceSheetQuestionY3)
  private val y3NY  : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY3, Seq(No, Yes)), companyEmployeesQuestionY3)
  private val y3NN  : (Seq[YesNoAnswer], Decision)      = (answers(companyY3, Seq(No, No)), Exempt("reason.company.notlargeenough"))
  private val y3NYY : (Seq[YesNoAnswer], YesNoQuestion) = (answers(companyY3, Seq(No, Yes, Yes)), hasSubsidiariesQuestion)
  private val y3YYn : (Seq[YesNoAnswer], Decision)      = (y3YY._1 :+ answer(hasSubsidiariesQuestion, No), Required)
  private val y3YYy : (Seq[YesNoAnswer], YesNoQuestion) = (y3YY._1 :+ answer(hasSubsidiariesQuestion, Yes), subsidiaryTurnoverQuestionY3)
  private val y3YYyY: (Seq[YesNoAnswer], YesNoQuestion) = (y3YYy._1 ++ answers(subsidiaryY3, Seq(Yes)), subsidiaryBalanceSheetQuestionY3)

  private val y3YYyYY : (Seq[YesNoAnswer], Decision)      = (y3YYy._1 ++ answers(subsidiaryY3, Seq(Yes, Yes)), Required)
  private val y3YYyYN : (Seq[YesNoAnswer], YesNoQuestion) = (y3YYy._1 ++ answers(subsidiaryY3, Seq(Yes, No)), subsidiaryEmployeesQuestionY3)
  private val y3YYyYNY: (Seq[YesNoAnswer], Decision)      = (y3YYy._1 ++ answers(subsidiaryY3, Seq(Yes, No, Yes)), Required)
  private val y3YYyYNN: (Seq[YesNoAnswer], Decision)      = (y3YYy._1 ++ answers(subsidiaryY3, Seq(Yes, No, No)), Exempt("reason.group.notlargeenough"))
  private val y3YYyN  : (Seq[YesNoAnswer], YesNoQuestion) = (y3YYy._1 ++ answers(subsidiaryY3, Seq(No)), subsidiaryBalanceSheetQuestionY3)
  private val y3YYyNY : (Seq[YesNoAnswer], YesNoQuestion) = (y3YYy._1 ++ answers(subsidiaryY3, Seq(No, Yes)), subsidiaryEmployeesQuestionY3)
  private val y3YYyNYY: (Seq[YesNoAnswer], Decision)      = (y3YYy._1 ++ answers(subsidiaryY3, Seq(No, Yes, Yes)), Required)
  private val y3YYyNYN: (Seq[YesNoAnswer], Decision)      = (y3YYy._1 ++ answers(subsidiaryY3, Seq(No, Yes, No)), Exempt("reason.group.notlargeenough"))
  private val y3YYyNN : (Seq[YesNoAnswer], Decision)      = (y3YYy._1 ++ answers(subsidiaryY3, Seq(No, No)), Exempt("reason.group.notlargeenough"))

  private val expectedQuestionsY3: Seq[(Seq[Answer], YesNoQuestion)] =
    Seq(y3Y, y3YY, y3YN, y3YNY, y3N, y3NY, y3NYY, y3YYy, y3YYyY, y3YYyYN, y3YYyN, y3YYyNY).map { case (as, q) =>
      (y3startState ++ as, q)
    }
  private val expectedDecisionsY3: Seq[(Seq[Answer], Decision)]      =
    Seq(y3NN, y3YYn, y3YYyYY, y3YYyYNY, y3YYyYNN, y3YYyNYY, y3YYyNYN, y3YYyNN).map { case (as, d) =>
      (y3startState ++ as, d)
    }


}


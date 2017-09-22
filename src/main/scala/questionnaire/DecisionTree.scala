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

import org.scalactic.TripleEquals._
import play.api.libs.json.Json._
import play.api.libs.json._
import utils.YesNo
import utils.YesNo.{No, Yes}

sealed trait DecisionTree
case class YesNoNode(question: YesNoQuestion, yes: DecisionTree, no: DecisionTree) extends DecisionTree {
  override def toString: String = s"YesNoNode(${question.id})"
}
case class YearNode(question: FinancialYearQuestion, y1: DecisionTree, y2: DecisionTree, y3: DecisionTree) extends DecisionTree {
  override def toString: String = s"YearNode(${question.id})"
}
case class DecisionNode(decision: Decision) extends DecisionTree {
  override def toString: String = s"DecisionNode($decision)"
}

sealed trait Answer {
  def questionId: Int
}
case class YesNoAnswer(questionId: Int, yesNo: YesNo) extends Answer
case class FinancialYearAnswer(questionId: Int, financialYear: FinancialYear) extends Answer

object Answer {

  val ynFormat: OFormat[YesNoAnswer]         = Json.format
  val fyFormat: OFormat[FinancialYearAnswer] = Json.format

  implicit val format: OFormat[Answer] = new OFormat[Answer] {
    override def writes(a: Answer): JsObject = a match {
      case yn: YesNoAnswer         => obj(("yesno", Json.toJson(yn)(ynFormat)))
      case fy: FinancialYearAnswer => obj(("financialyear", Json.toJson(fy)(fyFormat)))
    }

    override def reads(json: JsValue): JsResult[Answer] = {
      val obj = implicitly[Reads[JsObject]].reads(json)
      obj.flatMap { o =>
        val field = o.fields.headOption
        field match {
          case Some(("yesno", a))         => ynFormat.reads(a)
          case Some(("financialyear", a)) => fyFormat.reads(a)
          case _                          => JsError(s"could not decode $o")
        }
      }
    }
  }
}

object DecisionTree {

  import questionnaire.Questions._

  val groupNotLargeEnough = "reason.group.notlargeenough"

  val checkSubsidiariesTreeY2: DecisionTree =
    thresholdTree(subsidiaryTurnoverQuestionY2, subsidiaryBalanceSheetQuestionY2, subsidiaryEmployeesQuestionY2, DecisionNode(Required), DecisionNode(Exempt(groupNotLargeEnough)))
  val checkSubsidiariesTreeY3: DecisionTree =
    thresholdTree(subsidiaryTurnoverQuestionY3, subsidiaryBalanceSheetQuestionY3, subsidiaryEmployeesQuestionY3, DecisionNode(Required), DecisionNode(Exempt(groupNotLargeEnough)))

  val subsidiaryThresholdTreeY2: DecisionTree =
    YesNoNode(hasSubsidiariesQuestion, yes = checkSubsidiariesTreeY2, no = DecisionNode(Required))
  val subsidiaryThresholdTreeY3: DecisionTree =
    YesNoNode(hasSubsidiariesQuestion, yes = checkSubsidiariesTreeY3, no = DecisionNode(Required))

  val companyNotLargeEnough = "reason.company.notlargeenough"

  val checkCompanyTreeY2: DecisionTree =
    thresholdTree(companyTurnoverQuestionY2, companyBalanceSheetQuestionY2, companyEmployeesQuestionY2, subsidiaryThresholdTreeY2, DecisionNode(Exempt(companyNotLargeEnough)))
  val checkCompanyTreeY3: DecisionTree =
    thresholdTree(companyTurnoverQuestionY3, companyBalanceSheetQuestionY3, companyEmployeesQuestionY3, subsidiaryThresholdTreeY3, DecisionNode(Exempt(companyNotLargeEnough)))

  val companyYearTree = YearNode(financialYearQuestion, DecisionNode(Exempt("reason.firstyear")), checkCompanyTreeY2, checkCompanyTreeY3)

  val isCompanyOrLLPTree = YesNoNode(isCompanyOrLLPQuestion, companyYearTree, DecisionNode(NotACompany("reason.notacompany")))

  def thresholdTree(q1: YesNoQuestion, q2: YesNoQuestion, q3: YesNoQuestion, yesesHaveIt: DecisionTree, noesHaveIt: DecisionTree): DecisionTree =
    YesNoNode(
      q1,
      yes = YesNoNode(
        q2,
        yes = yesesHaveIt,
        no = YesNoNode(
          q3,
          yes = yesesHaveIt,
          no = noesHaveIt)),
      no = YesNoNode(
        q2,
        yes = YesNoNode(
          q3,
          yes = yesesHaveIt,
          no = noesHaveIt),
        no = noesHaveIt)
    )

  def checkAnswers(answers: Seq[Answer]): Either[String, DecisionTree] = {
    val start: Either[String, DecisionTree] = Right(isCompanyOrLLPTree)

    answers.foldLeft(start) {
      // If an earlier iteration returned an error then just return that error
      case (e@Left(_), _) => e

      case (Right(tree), answer) => (tree, answer) match {
        case (YesNoNode(qq, y, n), YesNoAnswer(aq, yn)) if qq.id === aq => Right {
          yn match {
            case Yes => y
            case No  => n
          }
        }

        case (YearNode(qq, y1, y2, y3), FinancialYearAnswer(aq, y)) if qq.id === aq => Right {
          y match {
            case FinancialYear.First        => y1
            case FinancialYear.Second       => y2
            case FinancialYear.ThirdOrLater => y3
          }
        }

        case (DecisionNode(decision), a) => Left(s"Expected a question for answer $a but got decision $decision")
        case (YesNoNode(q, _, _), a)     => Left(s"Answer $a did not match question ${q.id}")
        case (YearNode(q, _, _, _), a)   => Left(s"Answer $a did not match question ${q.id}")
      }
    }
  }
}
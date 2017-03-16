package questionnaire

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpecLike}

class DeciderTest extends WordSpecLike with Matchers with TableDrivenPropertyChecks {

  import DeciderTestData._


  val table = Table("test records", expectedDecisions: _*)

  "check decision for each state" in {
    forAll(table) { case (state, expectedDecision) =>
      Decider.calculateDecision(state) shouldBe expectedDecision
    }
  }
}

object DeciderTestData {

  import utils.YesNo._

  val empty = DecisionState.empty
  val s1 = empty.copy(isCompanyOrLLP = Some(Yes))
  val s2a = s1.copy(financialYear = Some(FinancialYear.First))

  val expectedDecisions: Seq[(DecisionState, Decision)] = Seq(
    (empty, AskQuestion(Questions.isCompanyOrLLPQuestion)),
    (s1, AskQuestion(Questions.financialYearQuestion)),
    (s2a, Exempt(Some("reason.firstyear")))
  )
}

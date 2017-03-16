package questionnaire

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpecLike}

class DeciderTest extends WordSpecLike with Matchers with TableDrivenPropertyChecks {

  import DeciderTestData._

  val decider = new Decider(questions)

  val table = Table("test records", expectedDecisions: _*)

  "check decision for each state" in {
    forAll(table) { case (state, expectedDecision) =>
      decider.calculateDecision(state) shouldBe expectedDecision
    }
  }

}

object DeciderTestData {

  import utils.YesNo._

  val questions = new Questions()(DummyMessages)
  private val empty = DecisionState.empty

  val expectedDecisions: Seq[(DecisionState, AskQuestion)] = Seq(
    (empty, AskQuestion(questions.isCompanyOrLLPQuestion)),
    (empty.copy(isCompanyOrLLP = Some(Yes)), AskQuestion(questions.financialYearQuestion))
  )
}

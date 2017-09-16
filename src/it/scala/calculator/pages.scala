package calculator

import controllers.routes
import play.api.mvc.Call
import webspec.{EntryPoint, PageInfo}

object CalculatorPage extends PageInfo with EntryPoint {
  override val title: String = "Calculate reporting periods and deadlines"
  override val call : Call   = routes.CalculatorController.start()
}

object ReportingPeriodsAndDeadlinesPage extends PageInfo {
  override val title: String = "Reporting periods and deadlines"
}
package reports

import controllers.{ReportController, routes}
import play.api.mvc.Call
import webspec.{EntryPoint, PageInfo}

object ReportingStartPage extends PageInfo with EntryPoint {
  override def name: String = "Search page"

  override def title: String = "Publish a report"

  override def call: Call = routes.ReportController.search(None, None, None)
}

case class PublishFor(companyName: String) extends PageInfo {
  override def name = s"Publish for $companyName"

  override def title: String = ReportController.publishTitle(companyName)
}
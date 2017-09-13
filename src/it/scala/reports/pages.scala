package reports

import controllers.{ReportController, ShortFormController, routes}
import play.api.mvc.Call
import services.CompanySearchResult
import webspec.{EntryPoint, PageInfo}

object ReportingStartPage extends PageInfo with EntryPoint {

  override val title: String = "Publish a report"

  override val call: Call = routes.ReportController.search(None, None, None)
}

case class PublishFor(companyName: String) extends PageInfo {

  override val title: String = ReportController.publishTitle(companyName)
}

case class ShortFormPage(company:CompanySearchResult) extends PageInfo {
  override val url: String = routes.ShortFormController.show(company.companiesHouseId, None).url

  override val title: String = ShortFormController.publishTitle(company.companyName)
}
package services

import com.google.inject.ImplementedBy
import com.wellfactored.playbindings.ValueClassFormats
import models.CompaniesHouseId
import play.api.libs.json.Json
import services.companiesHouse.CompaniesHouseSearch

import scala.concurrent.Future

case class CompanySearchResult(company_number: CompaniesHouseId, title: String, address_snippet: String)

object CompanySearchResult extends ValueClassFormats {
  implicit val fmt = Json.format[CompanySearchResult]
}

case class CompanyDetail(company_number: CompaniesHouseId, company_name: String)

object CompanyDetail extends ValueClassFormats {
  implicit val fmt = Json.format[CompanyDetail]
}

case class ResultsPage(
                        page_number: Int,
                        start_index: Int,
                        items_per_page: Int,
                        total_results: Int,
                        items: List[CompanySearchResult]
                      )

object ResultsPage {
  implicit val fmt = Json.format[ResultsPage]
}

@ImplementedBy(classOf[CompaniesHouseSearch])
trait CompanySearchService {
  def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[PagedResults[CompanySearchResult]]

  def find(companiesHouseId: CompaniesHouseId): Future[Option[CompanyDetail]]
}

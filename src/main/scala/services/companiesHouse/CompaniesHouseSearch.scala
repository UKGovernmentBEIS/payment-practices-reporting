package services.companiesHouse

import java.util.Base64
import javax.inject.Inject

import com.wellfactored.playbindings.ValueClassReads
import config.AppConfig
import models.CompaniesHouseId
import play.api.Logger
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.WSClient
import services._

import scala.concurrent.{ExecutionContext, Future}

class CompaniesHouseSearch @Inject()(val ws: WSClient, oAuth2Service: OAuth2Service, appConfig: AppConfig)(implicit val ec: ExecutionContext)
  extends RestService
    with CompanySearchService
    with ValueClassReads {

  import appConfig.config

  private val basicAuth = "Basic " + new String(Base64.getEncoder.encode(config.companiesHouse.apiKey.getBytes))

  implicit val companySummaryReads: Reads[CompanySummary] = Json.reads[CompanySummary]
  implicit val companyDetailReads: Reads[CompanyDetail] = Json.reads[CompanyDetail]
  implicit val resultsPageReads: Reads[ResultsPage] = Json.reads[ResultsPage]

  def targetScope(companiesHouseId: CompaniesHouseId): String = s"https://api.companieshouse.gov.uk/company/${companiesHouseId.id}"

  override def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[PagedResults[CompanySummary]] = {

    val s = views.html.helper.urlEncode(search)
    val startIndex = (page - 1) * itemsPerPage
    val url = s"https://api.companieshouse.gov.uk/search/companies?q=$s&items_per_page=$itemsPerPage&start_index=$startIndex"
    val start = System.currentTimeMillis()

    get[ResultsPage](url, basicAuth).map { resultsPage =>
      val t = System.currentTimeMillis() - start
      Logger.debug(s"Companies house search took ${t}ms")
      PagedResults(resultsPage.items, resultsPage.items_per_page, resultsPage.page_number, resultsPage.total_results)
    }
  }

  override def find(companiesHouseId: CompaniesHouseId): Future[Option[CompanyDetail]] = {
    val id = views.html.helper.urlEncode(companiesHouseId.id)
    val url = targetScope(companiesHouseId)

    getOpt[CompanyDetail](url, basicAuth)
  }
}

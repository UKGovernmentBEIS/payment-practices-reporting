package services

import com.google.inject.ImplementedBy
import models.CompaniesHouseId
import services.companiesHouse.CompaniesHouseSearch

import scala.concurrent.Future

@ImplementedBy(classOf[CompaniesHouseSearch])
trait CompanySearchService {
  def searchCompanies(search: String, page: Int, itemsPerPage: Int): Future[PagedResults[CompanySummary]]

  def find(companiesHouseId: CompaniesHouseId): Future[Option[CompanyDetail]]
}

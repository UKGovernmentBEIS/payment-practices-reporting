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

package controllers

import javax.inject.Inject

import cats.data.OptionT
import cats.instances.future._
import config.GoogleAnalyticsConfig
import models.{CompaniesHouseId, ReportId}
import org.joda.time.format.DateTimeFormat
import play.api.mvc.{Action, Controller}
import play.twirl.api.Html
import services._

import scala.concurrent.ExecutionContext

class SearchController @Inject()(
                                  val companySearch: CompanySearchService,
                                  val reportService: ReportService,
                                  val googleAnalytics: GoogleAnalyticsConfig)(implicit val ec: ExecutionContext)
  extends Controller
    with PageHelper
    with SearchHelper {

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  def start() = Action(Ok(page("Search payment practice reports")(views.html.search.start())))

  private val searchForReports = "Search for reports"
  val searchHeader = h1(searchForReports)
  val searchLink = routes.SearchController.search(None, None, None).url
  val searchPageTitle = "Search for a company"

  def companyLink(id: CompaniesHouseId, pageNumber: Option[Int]) = routes.SearchController.company(id, pageNumber).url

  def pageLink(query: Option[String], itemsPerPage: Option[Int], pageNumber: Int) = routes.ReportController.search(query, Some(pageNumber), itemsPerPage).url

  def search(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int]) = Action.async {
    def resultsPage(q: String, results: Option[PagedResults[CompanySearchResult]], countMap: Map[CompaniesHouseId, Int]): Html =
      page(searchPageTitle)(home, searchHeader, views.html.search.search(q, results, countMap, searchLink, companyLink(_, pageNumber), pageLink(query, itemsPerPage, _)))

    doSearch(query, pageNumber, itemsPerPage, resultsPage).map(Ok(_))
  }

  def company(companiesHouseId: CompaniesHouseId, pageNumber: Option[Int]) = Action.async { implicit request =>
    val pageLink = { i: Int => routes.SearchController.company(companiesHouseId, Some(i)).url }
    val result = for {
      co <- OptionT(companySearch.find(companiesHouseId))
      rs <- OptionT.liftF(reportService.byCompanyNumber(companiesHouseId).map(rs => PagedResults.page(rs.flatMap(_.filed), pageNumber.getOrElse(1))))
    } yield {
      val searchCrumb = Breadcrumb(routes.SearchController.search(None, None, None), searchForReports)
      val crumbs = breadcrumbs(homeBreadcrumb, searchCrumb)
      Ok(page(s"Payment practice reports for ${co.companyName}")(crumbs, views.html.search.company(co, rs, pageLink, df)))
    }

    result.value.map {
      case Some(r) => r
      case None => NotFound
    }
  }

  def view(reportId: ReportId) = Action.async { implicit request =>
    val f = for {
      report <- OptionT(reportService.findFiled(reportId))
    } yield {
      val searchCrumb = Breadcrumb(routes.SearchController.search(None, None, None), searchForReports)
      val companyCrumb = Breadcrumb(routes.SearchController.company(report.header.companyId, None), s"${report.header.companyName} reports")
      val crumbs = breadcrumbs(homeBreadcrumb, searchCrumb, companyCrumb)
      Ok(page(s"Payment practice report for ${report.header.companyName}")(crumbs, views.html.search.report(report, df)))
    }

    f.value.map {
      case Some(ok) => ok
      case None => NotFound
    }
  }
}

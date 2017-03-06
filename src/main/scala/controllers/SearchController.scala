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
import config.AppConfig
import models.{CompaniesHouseId, ReportId}
import org.joda.time.format.DateTimeFormat
import play.api.mvc.{Action, Controller}
import services.{CompanyAuthService, CompanySearchService, PagedResults}
import slicks.modules.ReportRepo

import scala.concurrent.{ExecutionContext, Future}

class SearchController @Inject()(
                                  companySearch: CompanySearchService,
                                  companyAuth: CompanyAuthService,
                                  reports: ReportRepo,
                                  val appConfig: AppConfig)(implicit ec: ExecutionContext)
  extends Controller
    with PageHelper {

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  def start() = Action(Ok(page("Search payment practice reports")(views.html.search.start())))

  private val searchForReports = "Search for reports"

  def search(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int]) = Action.async {
    val searchLink = routes.SearchController.search(None, None, None).url
    val pageLink = { i: Int => routes.SearchController.search(query, Some(i), itemsPerPage).url }
    val companyLink = { id: CompaniesHouseId => routes.SearchController.company(id, pageNumber).url }
    val header = h1(searchForReports)
    val title = "Search for a company"

    query match {
      case Some(q) => companySearch.searchCompanies(q, pageNumber.getOrElse(1), itemsPerPage.getOrElse(25)).flatMap { results =>
        val countsF = results.items.map { report =>
          reports.byCompanyNumber(report.company_number).map(rs => (report.company_number, rs.length))
        }

        Future.sequence(countsF).map { counts =>
          val countMap = Map(counts: _*)

          Ok(page(title)(home, header, views.html.search.search(q, Some(results), countMap, searchLink, companyLink, pageLink)))
        }
      }
      case None => Future.successful(Ok(page(title)(home, header, views.html.search.search("", None, Map.empty, searchLink, companyLink, pageLink))))
    }
  }

  def company(companiesHouseId: CompaniesHouseId, pageNumber: Option[Int]) = Action.async { implicit request =>
    val pageLink = { i: Int => routes.SearchController.company(companiesHouseId, Some(i)).url }
    val result = for {
      co <- OptionT(companySearch.find(companiesHouseId))
      rs <- OptionT.liftF(reports.byCompanyNumber(companiesHouseId).map(rs => PagedResults.page(rs.flatMap(_.filed), pageNumber.getOrElse(1))))
    } yield {
      val searchCrumb = Breadcrumb(routes.SearchController.search(None, None, None), searchForReports)
      val crumbs = breadcrumbs(homeBreadcrumb, searchCrumb)
      Ok(page(s"Payment practice reports for ${co.company_name}")(crumbs, views.html.search.company(co, rs, pageLink, df)))
    }

    result.value.map {
      case Some(r) => r
      case None => NotFound
    }
  }

  def view(reportId: ReportId) = Action.async { implicit request =>
    val f = for {
      report <- OptionT(reports.findFiled(reportId))
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

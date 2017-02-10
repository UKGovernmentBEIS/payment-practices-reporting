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
import models.{CompaniesHouseId, ReportId}
import org.joda.time.format.DateTimeFormat
import play.api.mvc.{Action, Controller}
import services.{CompaniesHouseAPI, PagedResults}
import slicks.modules.ReportRepo

import scala.concurrent.{ExecutionContext, Future}

class SearchController @Inject()(companiesHouseAPI: CompaniesHouseAPI, reports: ReportRepo)(implicit ec: ExecutionContext)
  extends Controller
    with PageHelper {

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  def search(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int]) = Action.async {
    val pageLink = { i: Int => routes.SearchController.search(query, Some(i), itemsPerPage) }

    query match {
      case Some(q) => companiesHouseAPI.searchCompanies(q, pageNumber.getOrElse(1), itemsPerPage.getOrElse(25)).flatMap { results =>
        val counts = results.items.map { report =>
          reports.byCompanyNumber(report.company_number).map(rs => (report.company_number, rs.length))
        }

        Future.sequence(counts).map { counts =>
          val countMap = Map(counts: _*)
          Ok(page(home, views.html.search.search(intentToFile = false, q, Some(results), countMap, pageLink)))
        }
      }
      case None => Future.successful(Ok(page(home, views.html.search.search(intentToFile = false, "", None, Map.empty, pageLink))))
    }
  }

  def company(companiesHouseId: CompaniesHouseId, pageNumber: Option[Int]) = Action.async { implicit request =>
    val pageLink = { i: Int => routes.SearchController.company(companiesHouseId, Some(i)) }
    val result = for {
      co <- OptionT(companiesHouseAPI.find(companiesHouseId))
      rs <- OptionT.liftF(reports.byCompanyNumber(companiesHouseId).map(PagedResults.page(_, pageNumber.getOrElse(1))))
    } yield {
      Ok(page(home, views.html.search.company(co, rs, pageLink, df)))
    }

    result.value.map {
      case Some(r) => r
      case None => NotFound
    }
  }

  def view(reportId: ReportId) = Action { implicit request => ??? }


}

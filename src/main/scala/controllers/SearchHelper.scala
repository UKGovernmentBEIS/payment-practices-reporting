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

import models.CompaniesHouseId
import play.twirl.api.Html
import services.{CompanySearchResult, CompanySearchService, PagedResults, ReportService}

import scala.concurrent.{ExecutionContext, Future}

trait SearchHelper {
  def companySearch: CompanySearchService

  def reports: ReportService

  implicit def ec: ExecutionContext

  type ResultsPageFunction = (String, Option[PagedResults[CompanySearchResult]], Map[CompaniesHouseId, Int]) => Html

  def doSearch(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int], resultsPage: ResultsPageFunction) = {
    query match {
      case Some(q) => companySearch.searchCompanies(q, pageNumber.getOrElse(1), itemsPerPage.getOrElse(25)).flatMap { results =>
        val countsF = results.items.map { report =>
          reports.byCompanyNumber(report.companiesHouseId).map(rs => (report.companiesHouseId, rs.count(_.isFiled)))
        }

        Future.sequence(countsF).map(counts => resultsPage(q, Some(results), Map(counts: _*)))
      }

      case None => Future.successful(resultsPage("", None, Map.empty))
    }
  }
}

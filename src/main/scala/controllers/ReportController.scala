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

import actions.SessionAction
import cats.data.OptionT
import cats.instances.future._
import config.{PageConfig, ServiceConfig}
import forms.Validations
import models.{CompaniesHouseId, ReportId}
import org.scalactic.TripleEquals._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import services.{ReportService, _}
import utils.YesNo

import scala.concurrent.ExecutionContext

object ReportController {
  val searchPageTitle = "Publish a report"
  val signInPageTitle = "Sign in using your Companies House account"

  val companyNumberParagraphId = "company-number"

  def publishTitle(companyName: String) = s"Publish a report for $companyName"

  val searchButtonId                            = "search-submit"
  val searchLink : String                       = routes.ReportController.search(None, None, None).url
  val companyLink: (CompaniesHouseId) => String = { id: CompaniesHouseId => routes.ReportController.start(id).url }
}

class ReportController @Inject()(
  companyAuth: CompanyAuthService,
  val companySearch: CompanySearchService,
  val reportService: ReportService,
  val pageConfig: PageConfig,
  val serviceConfig: ServiceConfig
)(implicit val ec: ExecutionContext, messages: MessagesApi)
  extends Controller
    with PageHelper
    with SearchHelper
    with CompanyHelper {

  import ReportController._
  import views.html.{report => pages}

  private val searchHeader = h1(searchPageTitle)

  private def pageLink(query: Option[String], itemsPerPage: Option[Int], pageNumber: Int) = routes.ReportController.search(query, Some(pageNumber), itemsPerPage).url

  //noinspection TypeAnnotation
  def search(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int]) = Action.async { implicit request =>
    val externalRouter = implicitly[ExternalRouter]

    def resultsPage(q: String, results: Option[PagedResults[CompanySearchResult]], countMap: Map[CompaniesHouseId, Int]) =
      page(searchPageTitle)(home, views.html.search.search(searchHeader, q, results, countMap, searchLink, companyLink, pageLink(query, itemsPerPage, _), externalRouter))

    doSearch(query, pageNumber, itemsPerPage, resultsPage).map(Ok(_))
  }

  //noinspection TypeAnnotation
  def start(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>
    val backCrumb = breadcrumbs("link-back", Breadcrumb(routes.ReportController.search(None, None, None).url, "Back"))
    companySearch.find(companiesHouseId).map {
      case Some(co) => Ok(page(publishTitle(co.companyName))(backCrumb, pages.start(co.companyName, co.companiesHouseId)))
      case None     => NotFound(s"Could not find a company with id ${companiesHouseId.id}")
    }
  }

  val hasAccountChoice = Form(single("account" -> Validations.yesNo))

  def preLogin(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    val backCrumb = breadcrumbs("link-back", Breadcrumb(routes.ReportController.start(companiesHouseId).url, "Back"))
    Ok(page(signInPageTitle)(backCrumb, pages.preLogin(companiesHouseId, hasAccountChoice))).removingFromSession(SessionAction.sessionIdKey)
  }

  def login(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    hasAccountChoice.bindFromRequest().fold(
      errs => BadRequest(page(signInPageTitle)(home, pages.preLogin(companiesHouseId, errs))),
      hasAccount =>
        if (hasAccount === YesNo.Yes) Redirect(companyAuth.authoriseUrl(companiesHouseId), companyAuth.authoriseParams(companiesHouseId))
        else Redirect(routes.CoHoCodeController.code(companiesHouseId))
    )
  }

  //noinspection TypeAnnotation
  def colleague(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>
    withCompany(companiesHouseId)(co => page("If you want a colleague to publish a report")(home, pages.askColleague(co.companyName, companiesHouseId)))
  }

  //noinspection TypeAnnotation
  def register(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>
    withCompany(companiesHouseId)(co => page("Request an authentication code")(home, pages.requestAccessCode(co.companyName, companiesHouseId)))
  }

  def applyForAuthCode(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    Redirect(companyAuth.authoriseUrl(companiesHouseId), companyAuth.authoriseParams(companiesHouseId))
  }

  //noinspection TypeAnnotation
  def view(reportId: ReportId) = Action.async { implicit request =>
    val f = for {
      report <- OptionT(reportService.find(reportId))
    } yield {
      val crumbs = breadcrumbs("", homeBreadcrumb)
      Ok(page(s"Payment practice report for ${report.companyName}")(crumbs, views.html.search.report(report, df)))
    }

    f.value.map {
      case Some(ok) => ok
      case None     => NotFound
    }
  }
}


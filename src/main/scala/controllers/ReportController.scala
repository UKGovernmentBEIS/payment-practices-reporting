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
import config.AppConfig
import controllers.ReportController.CodeOption.{Colleague, Register}
import forms.Validations
import models.CompaniesHouseId
import org.scalactic.TripleEquals._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller, Result}
import play.twirl.api.Html
import services.{ReportService, _}
import utils.YesNo

import scala.concurrent.{ExecutionContext, Future}

class ReportController @Inject()(
                                  companyAuth: CompanyAuthService,
                                  val companySearch: CompanySearchService,
                                  val reports: ReportService,
                                  val appConfig: AppConfig
                                )(implicit val ec: ExecutionContext, messages: MessagesApi)
  extends Controller
    with PageHelper
    with SearchHelper {

  import views.html.{report => pages}

  private val searchPageTitle = "Search for a company"
  private val signInPageTitle = "Sign in using your Companies House account"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  val searchHeader = h1("Publish a report")
  val searchLink = routes.ReportController.search(None, None, None).url
  val companyLink = { id: CompaniesHouseId => routes.ReportController.start(id).url }

  def pageLink(query: Option[String], itemsPerPage: Option[Int], pageNumber: Int) = routes.ReportController.search(query, Some(pageNumber), itemsPerPage).url

  def search(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int]) = Action.async {
    def resultsPage(q: String, results: Option[PagedResults[CompanySearchResult]], countMap: Map[CompaniesHouseId, Int]) =
      page(searchPageTitle)(home, searchHeader, views.html.search.search(q, results, countMap, searchLink, companyLink, pageLink(query, itemsPerPage, _)))

    doSearch(query, pageNumber, itemsPerPage, resultsPage).map(Ok(_))
  }

  def start(companiesHouseId: CompaniesHouseId) = Action.async { request =>
    companySearch.find(companiesHouseId).map {
      case Some(co) => Ok(page(publishTitle(co.companyName))(home, pages.start(co.companyName, co.companiesHouseId)))
      case None => NotFound(s"Could not find a company with id ${companiesHouseId.id}")
    }
  }

  def preLogin(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    Ok(page(signInPageTitle)(home, pages.preLogin(companiesHouseId))).removingFromSession(SessionAction.sessionIdKey)
  }

  def login(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    val hasAccountChoice = Form(single("account" -> Validations.yesNo))

    hasAccountChoice.bindFromRequest().fold(
      errs => BadRequest(page(signInPageTitle)(home, pages.preLogin(companiesHouseId))),
      hasAccount =>
        if (hasAccount === YesNo.Yes) Redirect(companyAuth.authoriseUrl(companiesHouseId), companyAuth.authoriseParams(companiesHouseId))
        else Redirect(routes.ReportController.code(companiesHouseId))
    )
  }

  def withCompany(companiesHouseId: CompaniesHouseId)(body: CompanyDetail => Html): Future[Result] = {
    companySearch.find(companiesHouseId).map {
      case Some(co) => Ok(body(co))
      case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
    }
  }

  def code(companiesHouseId: CompaniesHouseId) = Action.async {
    withCompany(companiesHouseId)(co => page("If you don't have a Companies House authentication code")(home, pages.companiesHouseOptions(co.companyName, companiesHouseId)))
  }

  def colleague(companiesHouseId: CompaniesHouseId) = Action.async {
    withCompany(companiesHouseId)(co => page("If you want a colleague to publish a report")(home, pages.askColleague(co.companyName, companiesHouseId)))
  }

  def register(companiesHouseId: CompaniesHouseId) = Action.async {
    withCompany(companiesHouseId)(co => page("Request an authentication code")(home, pages.requestAccessCode(co.companyName, companiesHouseId)))
  }

  def codeOptions(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    import ReportController.CodeOption

    def resultFor(codeOption: CodeOption) = codeOption match {
      case Colleague => Redirect(routes.ReportController.colleague(companiesHouseId))
      case Register => Redirect(routes.ReportController.register(companiesHouseId))
    }

    val form = Form(single("nextstep" -> Forms.of[CodeOption]))

    form.bindFromRequest().fold(errs => BadRequest(s"Invalid option"), resultFor)
  }
}

object ReportController {

  import enumeratum.EnumEntry.Lowercase
  import enumeratum._
  import utils.EnumFormatter

  sealed trait CodeOption extends EnumEntry with Lowercase

  object CodeOption extends Enum[CodeOption] with EnumFormatter[CodeOption] {
    override def values = findValues

    case object Colleague extends CodeOption

    case object Register extends CodeOption

  }

}
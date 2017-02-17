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

import actions.{CompanyAction, CompanyRequest}
import forms.Validations
import forms.report.{ReportFormModel, ReportReviewModel, Validations}
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller, Result}
import services.CompaniesHouseAPI
import slicks.modules.ReportRepo
import utils.{TimeSource, YesNo}

import scala.concurrent.{ExecutionContext, Future}

class ReportController @Inject()(
                                  companiesHouseAPI: CompaniesHouseAPI,
                                  reports: ReportRepo,
                                  timeSource: TimeSource,
                                  CompanyAction: CompanyAction
                                )(implicit ec: ExecutionContext, messages: MessagesApi) extends Controller with PageHelper {

  import views.html.{report => pages}

  private val reportValidations = new Validations(timeSource)
  val emptyReport: Form[ReportFormModel] = Form(reportValidations.reportFormModel)
  val emptyReview: Form[ReportReviewModel] = Form(reportValidations.reportReviewModel)
  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  def search(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int]) = Action.async {
    val searchLink = routes.ReportController.search(None, None, None)
    val pageLink = { i: Int => routes.ReportController.search(query, Some(i), itemsPerPage) }
    val companyLink = { id: CompaniesHouseId => routes.ReportController.start(id) }
    val header = h1("Publish a report")

    query match {
      case Some(q) => companiesHouseAPI.searchCompanies(q, pageNumber.getOrElse(1), itemsPerPage.getOrElse(25)).flatMap { results =>
        val countsF = results.items.map { report =>
          reports.byCompanyNumber(report.company_number).map(rs => (report.company_number, rs.length))
        }

        Future.sequence(countsF).map { counts =>
          val countMap = Map(counts: _*)
          Ok(page(home, header, views.html.search.search(q, Some(results), countMap, searchLink, companyLink, pageLink)))
        }
      }
      case None => Future.successful(Ok(page(home, header, views.html.search.search("", None, Map.empty, searchLink, companyLink, pageLink))))
    }
  }

  def start(companiesHouseId: CompaniesHouseId) = Action.async { request =>
    companiesHouseAPI.find(companiesHouseId).map {
      case Some(co) => Ok(page(home, pages.start(co.company_name, co.company_number)))
      case None => NotFound(s"Could not find a company with id ${companiesHouseId.id}")
    }
  }

  def preLogin(companiesHouseId: CompaniesHouseId) = Action { request =>
    Ok(page(home, pages.signInInterstitial(companiesHouseId)))
  }

  def login(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    val hasAccountChoice = Form(mapping("account" -> Validations.yesNo)(identity)(b => Some(b)))

    val next = hasAccountChoice.bindFromRequest().fold(
      errs => routes.ReportController.preLogin(companiesHouseId),
      hasAccount =>
        if (hasAccount == YesNo.Yes) routes.CoHoOAuthMockController.login(companiesHouseId)
        else routes.ReportController.code(companiesHouseId)
    )

    Redirect(next)
  }

  def code(companiesHouseId: CompaniesHouseId) = Action.async { request =>
    companiesHouseAPI.find(companiesHouseId).map {
      case Some(co) => Ok(page(home, pages.companiesHouseOptions(co.company_name, companiesHouseId)))
      case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
    }
  }

  def colleague(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>
    companiesHouseAPI.find(companiesHouseId).map {
      case Some(co) => Ok(page(home, pages.askColleague(co.company_name, companiesHouseId)))
      case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
    }
  }

  def register(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>
    companiesHouseAPI.find(companiesHouseId).map {
      case Some(co) => Ok(page(home, pages.requestAccessCode(co.company_name, companiesHouseId)))
      case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
    }
  }

  def codeOptions(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    import ReportController.CodeOption
    import CodeOption._

    val codeOption: Mapping[CodeOption] = Forms.of[CodeOption]
    val form = Form(mapping("nextstep" -> codeOption)(identity)(o => Some(o)))

    form.bindFromRequest().fold(
      errs => BadRequest(s"Invalid option"),
      {
        case Colleague => Redirect(routes.ReportController.colleague(companiesHouseId))
        case Register => Redirect(routes.ReportController.register(companiesHouseId))
      }
    )
  }

  def header(implicit request: CompanyRequest[_]) = h1(s"Publish a report for ${request.companyName}")

  def file(companiesHouseId: CompaniesHouseId) = CompanyAction(companiesHouseId) { implicit request =>
    Ok(page(home, header, pages.file(emptyReport, companiesHouseId, LocalDate.now(), df)))
  }

  def postForm(companiesHouseId: CompaniesHouseId) = CompanyAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    println(request.body.flatMap { case (k,v) =>
        v.headOption.map(value => s""""$k" -> "$value"""")
    }.mkString(","))
    emptyReport.bindFromRequest().fold(
      errs => BadRequest(page(home, header, pages.file(errs, companiesHouseId, LocalDate.now(), df))),
      report => Ok(page(home, pages.review(emptyReview, report, companiesHouseId, request.companyName, df, reportValidations.reportFormModel)))
    )
  }

  def postReview(companiesHouseId: CompaniesHouseId) = CompanyAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val revise = request.body.get("revise").flatMap(_.headOption).contains("Revise")
    // Re-capture the values for the report itself. In theory these values should always be valid
    // (as we only send the user to the review page if they are) but if somehow they aren't then
    // send the user back to the report form to fix them.
    emptyReport.bindFromRequest().fold(
      errs => Future.successful(BadRequest(page(home, header, pages.file(errs, companiesHouseId, LocalDate.now(), df)))),
      report =>
        if (revise) Future.successful(Ok(page(home, header, pages.file(emptyReport.fill(report), companiesHouseId, LocalDate.now(), df))))
        else checkConfirmation(companiesHouseId, report)
    )
  }

  private def checkConfirmation(companiesHouseId: CompaniesHouseId, report: ReportFormModel)(implicit request: CompanyRequest[_]): Future[Result] = {
    emptyReview.bindFromRequest().fold(
      errs => Future.successful(BadRequest(page(home, pages.review(errs, report, companiesHouseId, request.companyName, df, reportValidations.reportFormModel)))),
      confirmation =>
        if (confirmation.confirmed) reports.save(confirmation.confirmedBy, companiesHouseId, request.companyName, report).map { reportId =>
          Redirect(controllers.routes.ReportController.showConfirmation(reportId))
        }
        else Future.successful(BadRequest(page(home, pages.review(emptyReview.fill(confirmation), report, companiesHouseId, request.companyName, df, reportValidations.reportFormModel))))
    )
  }

  def showConfirmation(reportId: ReportId) = Action {
    Ok(page(home, pages.filingSuccess(reportId, "<unknown>")))
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
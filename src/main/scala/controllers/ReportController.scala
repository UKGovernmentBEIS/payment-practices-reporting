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

import actions.{CompanyAuthAction, CompanyAuthRequest}
import controllers.ReportController.CodeOption.{Colleague, Register}
import forms.Validations
import forms.report.{ReportFormModel, ReportReviewModel, Validations}
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller, Result}
import play.twirl.api.Html
import services.{CompaniesHouseAPI, CompanyDetail, NotifyService}
import slicks.modules.ReportRepo
import utils.{TimeSource, YesNo}

import scala.concurrent.{ExecutionContext, Future}

class ReportController @Inject()(
                                  companiesHouseAPI: CompaniesHouseAPI,
                                  notifyService: NotifyService,
                                  reports: ReportRepo,
                                  timeSource: TimeSource,
                                  CompanyAuthAction: CompanyAuthAction
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

  def preLogin(companiesHouseId: CompaniesHouseId) = Action(Ok(page(home, pages.preLogin(companiesHouseId))))

  def login(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    val hasAccountChoice = Form(single("account" -> Validations.yesNo))

    val next = hasAccountChoice.bindFromRequest().fold(
      errs => routes.ReportController.preLogin(companiesHouseId),
      hasAccount =>
        if (hasAccount == YesNo.Yes) routes.CoHoOAuthMockController.login(companiesHouseId)
        else routes.ReportController.code(companiesHouseId)
    )

    Redirect(next)
  }

  def withCompany(companiesHouseId: CompaniesHouseId)(body: CompanyDetail => Html): Future[Result] = {
    companiesHouseAPI.find(companiesHouseId).map {
      case Some(co) => Ok(body(co))
      case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
    }
  }

  def code(companiesHouseId: CompaniesHouseId) = Action.async {
    withCompany(companiesHouseId)(co => page(home, pages.companiesHouseOptions(co.company_name, companiesHouseId)))
  }

  def colleague(companiesHouseId: CompaniesHouseId) = Action.async {
    withCompany(companiesHouseId)(co => page(home, pages.askColleague(co.company_name, companiesHouseId)))
  }

  def register(companiesHouseId: CompaniesHouseId) = Action.async {
    withCompany(companiesHouseId)(co => page(home, pages.requestAccessCode(co.company_name, companiesHouseId)))
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

  def reportPageHeader(implicit request: CompanyAuthRequest[_]) = h1(s"Publish a report for ${request.companyName}")

  def file(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId) { implicit request =>
    Ok(page(home, reportPageHeader, pages.file(emptyReport, companiesHouseId, LocalDate.now(), df)))
  }

  def postForm(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    //println(request.body.flatMap { case (k, v) => v.headOption.map(value => s""""$k" -> "$value"""") }.mkString(", "))
    emptyReport.bindFromRequest().fold(
      errs => BadRequest(page(home, reportPageHeader, pages.file(errs, companiesHouseId, LocalDate.now(), df))),
      report => Ok(page(home, pages.review(emptyReview, report, companiesHouseId, request.companyName, df, reportValidations.reportFormModel)))
    )
  }

  def postReview(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val revise = Form(single("revise" -> text)).bindFromRequest().value.contains("Revise")

    // Re-capture the values for the report itself. In theory these values should always be valid
    // (as we only send the user to the review page if they are) but if somehow they aren't then
    // send the user back to the report form to fix them.
    emptyReport.bindFromRequest().fold(
      errs => Future.successful(BadRequest(page(home, reportPageHeader, pages.file(errs, companiesHouseId, LocalDate.now(), df)))),
      report =>
        if (revise) Future.successful(Ok(page(home, reportPageHeader, pages.file(emptyReport.fill(report), companiesHouseId, LocalDate.now(), df))))
        else checkConfirmation(companiesHouseId, report)
    )
  }

  private def checkConfirmation(companiesHouseId: CompaniesHouseId, report: ReportFormModel)(implicit request: CompanyAuthRequest[_]): Future[Result] = {
    emptyReview.bindFromRequest().fold(
      errs => Future.successful(BadRequest(page(home, pages.review(errs, report, companiesHouseId, request.companyName, df, reportValidations.reportFormModel)))),
      review => {
        if (review.confirmed)
          createReport(companiesHouseId, report, request, review)
            .map(rId => Redirect(controllers.routes.ReportController.showConfirmation(rId)))
        else
          Future.successful(BadRequest(page(home, pages.review(emptyReview.fill(review), report, companiesHouseId, request.companyName, df, reportValidations.reportFormModel))))
      }
    )
  }

  private def createReport(companiesHouseId: CompaniesHouseId, report: ReportFormModel, request: CompanyAuthRequest[_], review: ReportReviewModel): Future[ReportId] = {
    for {
      reportId <- reports.create(review.confirmedBy, companiesHouseId, request.companyName, report, review)
    } yield reportId
  }

  def showConfirmation(reportId: ReportId) = Action(Ok(page(home, pages.filingSuccess(reportId, "<unknown>"))))
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
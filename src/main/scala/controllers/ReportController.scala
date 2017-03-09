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

import javax.inject.{Inject, Named}

import actions.{CompanyAuthAction, CompanyAuthRequest, SessionAction}
import akka.actor.ActorRef
import config.AppConfig
import controllers.ReportController.CodeOption.{Colleague, Register}
import forms.Validations
import forms.report.{ReportFormModel, ReportReviewModel, Validations}
import models.{CompaniesHouseId, ReportId}
import org.joda.time.format.DateTimeFormat
import org.scalactic.TripleEquals._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller, Result}
import play.twirl.api.Html
import services._
import slicks.modules.ReportRepo
import utils.YesNo

import scala.concurrent.{ExecutionContext, Future}

class ReportController @Inject()(
                                  companySearch: CompanySearchService,
                                  companyAuth: CompanyAuthService,
                                  notifyService: NotifyService,
                                  reports: ReportRepo,
                                  reportValidations: Validations,
                                  oAuthController: OAuth2Controller,
                                  CompanyAuthAction: CompanyAuthAction,
                                  val appConfig: AppConfig,
                                  @Named("confirmation-actor") confirmationActor: ActorRef
                                )(implicit ec: ExecutionContext, messages: MessagesApi) extends Controller with PageHelper {

  import views.html.{report => pages}

  val emptyReport: Form[ReportFormModel] = Form(reportValidations.reportFormModel)
  val emptyReview: Form[ReportReviewModel] = Form(reportValidations.reportReviewModel)
  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  private val searchPageTitle = "Search for a company"
  private val signInPageTitle = "Sign in using your Companies House account"
  private val reviewPageTitle = "Review your report"

  def search(query: Option[String], pageNumber: Option[Int], itemsPerPage: Option[Int]) = Action.async {
    val searchLink = routes.ReportController.search(None, None, None).url
    val pageLink = { i: Int => routes.ReportController.search(query, Some(i), itemsPerPage).url }
    val companyLink = { id: CompaniesHouseId => routes.ReportController.start(id).url }
    val header = h1("Publish a report")


    query match {
      case Some(q) => companySearch.searchCompanies(q, pageNumber.getOrElse(1), itemsPerPage.getOrElse(25)).flatMap { results =>
        val countsF = results.items.map { report =>
          reports.byCompanyNumber(report.companiesHouseId).map(rs => (report.companiesHouseId, rs.length))
        }

        Future.sequence(countsF).map { counts =>
          val countMap = Map(counts: _*)
          Ok(page(searchPageTitle)(home, header, views.html.search.search(q, Some(results), countMap, searchLink, companyLink, pageLink)))
        }
      }
      case None => Future.successful(Ok(page(searchPageTitle)(home, header, views.html.search.search("", None, Map.empty, searchLink, companyLink, pageLink))))
    }
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
        if (hasAccount === YesNo.Yes) oAuthController.startOauthDance(companiesHouseId)
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

  def reportPageHeader(implicit request: CompanyAuthRequest[_]) = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def file(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId) { implicit request =>
    Ok(page(publishTitle(request.companyDetail.companyName))(home, reportPageHeader, pages.file(emptyReport, companiesHouseId, df)))
  }

  def postForm(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    //println(request.body.flatMap { case (k, v) => v.headOption.map(value => s""""$k" -> "$value"""") }.mkString(", "))
    emptyReport.bindFromRequest().fold(
      errs => BadRequest(page(publishTitle(request.companyDetail.companyName))(home, reportPageHeader, pages.file(errs, companiesHouseId, df))),
      report => Ok(page(reviewPageTitle)(home, pages.review(emptyReview, report, companiesHouseId, request.companyDetail.companyName, df, reportValidations.reportFormModel)))
    )
  }

  def postReview(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val revise = Form(single("revise" -> text)).bindFromRequest().value.contains("Revise")

    // Re-capture the values for the report itself. In theory these values should always be valid
    // (as we only send the user to the review page if they are) but if somehow they aren't then
    // send the user back to the report form to fix them.
    emptyReport.bindFromRequest().fold(
      errs => Future.successful(BadRequest(page(publishTitle(request.companyDetail.companyName))(home, reportPageHeader, pages.file(errs, companiesHouseId, df)))),
      report =>
        if (revise) Future.successful(Ok(page(publishTitle(request.companyDetail.companyName))(home, reportPageHeader, pages.file(emptyReport.fill(report), companiesHouseId, df))))
        else checkConfirmation(companiesHouseId, report)
    )
  }

  private def checkConfirmation(companiesHouseId: CompaniesHouseId, report: ReportFormModel)(implicit request: CompanyAuthRequest[_]): Future[Result] = {
    emptyReview.bindFromRequest().fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, report, companiesHouseId, request.companyDetail.companyName, df, reportValidations.reportFormModel)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(companiesHouseId, request.oAuthToken) {
          createReport(companiesHouseId, report, review).map(rId => Redirect(controllers.routes.ReportController.showConfirmation(rId)))
        }
        else
          Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), report, companiesHouseId, request.companyDetail.companyName, df, reportValidations.reportFormModel))))
      }
    )
  }

  private def verifyingOAuthScope(companiesHouseId: CompaniesHouseId, oAuthToken: OAuthToken)(body: => Future[Result]): Future[Result] = {
    companyAuth.isInScope(companiesHouseId, oAuthToken).flatMap {
      case true => body
      case false => Future.successful(Redirect(controllers.routes.ReportController.invalidScope(companiesHouseId)))
    }
  }

  private def createReport(companiesHouseId: CompaniesHouseId, report: ReportFormModel, review: ReportReviewModel)(implicit request: CompanyAuthRequest[_]): Future[ReportId] = {
    val urlFunction: ReportId => String = (id: ReportId) => controllers.routes.SearchController.view(id).absoluteURL()
    for {
      reportId <- reports.create(review.confirmedBy, companiesHouseId, request.companyDetail.companyName, report, review, request.emailAddress, urlFunction)
      _ <- Future.successful(confirmationActor ! 'poll)
    } yield reportId
  }

  def showConfirmation(reportId: ReportId) = Action.async { implicit request =>
    reports.findFiled(reportId).map {
      case Some(report) => Ok(page(s"You have published a report for ${report.header.companyName}")(home, pages.filingSuccess(reportId, report.filing.confirmationEmailAddress)))
      case None => BadRequest(s"Could not find a report with id ${reportId.id}")
    }
  }

  def invalidScope(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId) { implicit request =>
    Ok(page("Your report has not been filed because of an error")(home, pages.invalidScope(request.companyDetail)))
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
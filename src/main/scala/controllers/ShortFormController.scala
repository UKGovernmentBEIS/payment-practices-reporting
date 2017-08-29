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

import actions.{CompanyAuthAction, CompanyAuthRequest}
import akka.actor.ActorRef
import cats.data.OptionT
import cats.instances.future._
import config.{PageConfig, ServiceConfig}
import controllers.FormPageModels._
import forms.report._
import models.{CompaniesHouseId, ReportId}
import play.api.data.Form
import play.api.data.Forms.{single, _}
import play.api.i18n.MessagesApi
import play.api.mvc.{Call, Controller, Result}
import play.twirl.api.Html
import services._
import views.html.helpers.ReviewPageData

import scala.concurrent.{ExecutionContext, Future}

class ShortFormController @Inject()(
  reports: ReportService,
  validations: Validations,
  val companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  formModel: ShortFormPageModel,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi) extends Controller with BaseFormController with PageHelper with FormSessionHelpers {

  import validations._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  //noinspection TypeAnnotation
  def show(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)

    checkValidFromSession(emptyReportingPeriod, ShortFormName.ReportingPeriod.entryName).flatMap {
      case false => Future.successful(Redirect(routes.ReportingPeriodController.show(companiesHouseId)))
      case true  => loadFormData(emptyShortForm, ShortFormName.ShortForm).map { form =>
        Ok(page(title)(home, pages.shortForm(reportPageHeader, form, companiesHouseId, df, serviceStartDate)))
      }
    }
  }

  //noinspection TypeAnnotation
  def post(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)
    val action = routes.ShortFormController.postReview(companiesHouseId)

    val shortForm = emptyShortForm.bindForm
    saveFormData(ShortFormName.ShortForm, shortForm).flatMap { _ =>
      checkValidFromSession(emptyReportingPeriod, ShortFormName.ReportingPeriod.entryName).map {
        case false => Redirect(routes.ReportingPeriodController.show(companiesHouseId))
        case true  => shortForm.fold(
          errs => Redirect(routes.ShortFormController.show(companiesHouseId)),
          sf => Redirect(routes.ShortFormController.showReview(companiesHouseId))
        )
      }
    }
  }

  //noinspection TypeAnnotation
  def showReview(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async { implicit request =>
    val action: Call = routes.ShortFormController.postReview(companiesHouseId)
    for {
      reportingPeriod <- loadFormData(emptyReportingPeriod, ShortFormName.ReportingPeriod)
      sf <- loadFormData(emptyShortForm, ShortFormName.ShortForm)
    } yield {
      val formGroups = ReviewPageData.formGroups(request.companyDetail.companyName, reportingPeriod.get, sf.get)
      Ok(page(reviewPageTitle)(home, pages.review(emptyReview, formGroups, action)))
    }
  }

  //noinspection TypeAnnotation
  def postReview(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val revise: Boolean = Form(single("revise" -> text)).bindForm.value.contains("Revise")
    val action: Call = routes.ShortFormController.postReview(companiesHouseId)

    if (revise) Future.successful(Redirect(routes.ReportingPeriodController.show(companiesHouseId)))
    else bindAllPages(formModel.formHandlers).flatMap {
      case FormHasErrors(handler) => Future.successful(Redirect(handler.pageCall(request.companyDetail)))
      case FormIsBlank(handler)   => Future.successful(Redirect(handler.pageCall(request.companyDetail)))
      case FormIsOk(handler)      =>
        val forms = for {
          reportingPeriod <- OptionT(loadFormData(emptyReportingPeriod, ShortFormName.ReportingPeriod).map(_.value))
          shortForm <- OptionT(loadFormData(emptyShortForm, ShortFormName.ShortForm).map(_.value))
        } yield (reportingPeriod, shortForm)
        forms.value.flatMap {
          case None          => ???
          case Some((r, sf)) =>
            val formGroups = ReviewPageData.formGroups(request.companyDetail.companyName, r, sf)

            emptyReview.bindForm.fold(
              errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, formGroups, action)))),
              review => {
                if (review.confirmed) verifyingOAuthScope(companiesHouseId, request.oAuthToken) {
                  createReport(companiesHouseId, r, sf, review.confirmedBy).map(rId => Redirect(controllers.routes.ConfirmationController.showConfirmation(rId)))
                } else {
                  Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), formGroups, action))))
                }
              }
            )
        }
    }
  }

  private def checkConfirmation(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, shortForm: ShortFormModel)
    (implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Future[Result] = {
    val action: Call = routes.ShortFormController.postReview(companiesHouseId)
    val companyName: String = request.companyDetail.companyName

    val reportData = emptyReportingPeriod.fill(reportingPeriod).data ++ emptyShortForm.fill(shortForm).data
    val formGroups = ReviewPageData.formGroups(companyName, reportingPeriod, shortForm)

    emptyReview.bindForm.fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, formGroups, action)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(companiesHouseId, request.oAuthToken) {
          createReport(companiesHouseId, reportingPeriod, shortForm, review.confirmedBy).map(rId => Redirect(controllers.routes.ConfirmationController.showConfirmation(rId)))
        } else {
          Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), formGroups, action))))
        }
      }
    )
  }

  private def createReport(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, shortForm: ShortFormModel, confirmedBy: String)(implicit request: CompanyAuthRequest[_]): Future[ReportId] = {
    val urlFunction: ReportId => String = (id: ReportId) => controllers.routes.ReportController.view(id).absoluteURL()
    for {
      reportId <- reports.createShortReport(request.companyDetail, reportingPeriod, shortForm, confirmedBy, request.emailAddress, urlFunction)
      _ <- Future.successful(confirmationActor ! 'poll)
    } yield reportId
  }
}

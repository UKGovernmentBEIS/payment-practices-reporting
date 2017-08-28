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
import controllers.PagedLongFormModel.FormName
import controllers.PagedLongFormModel.FormName._
import forms.report.{LongFormModel, Validations}
import models.CompaniesHouseId
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.twirl.api.Html
import services._
import views.html.helpers.ReviewPageData

import scala.concurrent.{ExecutionContext, Future}
import scala.language.existentials

class PagedLongFormController @Inject()(
  reports: ReportService,
  validations: Validations,
  pageFormData: PagedLongFormData,
  val companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  reportingPeriodController: ReportingPeriodController,
  pagedLongFormModel: PagedLongFormModel,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi) extends Controller with BaseFormController with PageHelper with FormSessionHelpers {

  import pageFormData._
  import validations._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  sealed trait FormResult
  case class FormIsBlank(formHandler: FormHandler[_]) extends FormResult
  case class FormHasErrors(formHandler: FormHandler[_]) extends FormResult
  case class FormIsOk(formHandler: FormHandler[_]) extends FormResult

  def show(formName: FormName, companiesHouseId: CompaniesHouseId): Action[AnyContent] = companyAuthAction(companiesHouseId).async { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)

    val handlerForThisPage = pagedLongFormModel.handlerFor(formName)

    bindUpToPage(formName).map {
      case FormIsOk(handler)      => Ok(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
      case FormHasErrors(handler) =>
        if (handler.formName == handlerForThisPage.formName)
          BadRequest(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
        else
          Redirect(handler.pageCall(request.companyDetail))
      case FormIsBlank(handler)   =>
        if (handler.formName == handlerForThisPage.formName)
          Ok(page(title)(handlerForThisPage.renderPage(reportPageHeader, request.companyDetail)))
        else
          Redirect(handler.pageCall(request.companyDetail))
    }
  }

  /**
    * Bind pages up to including the page with the given `formName`, returning the first result that
    * is empty or fails validation, or an Ok result for the named form.
    */
  private def bindUpToPage(formName: FormName)(implicit request: CompanyAuthRequest[_]): Future[FormResult] = {
    val (handlersToBind, _) = pagedLongFormModel.formHandlers.splitAt(pagedLongFormModel.formHandlers.indexWhere(_.formName == formName) + 1)

    loadAllFormData.map { data =>
      handlersToBind.foldLeft(Seq.empty[FormResult]) { (results, handler) =>
        results.headOption match {
          case Some(FormHasErrors(_)) | Some(FormIsBlank(_)) => results
          case _                                             => bindPage(data, handler) +: results
        }
      }.head
    }
  }

  private def bindAllPages(implicit request: CompanyAuthRequest[_]): Future[FormResult] = {
    loadAllFormData.map { data =>
      pagedLongFormModel.formHandlers.foldLeft(Seq.empty[FormResult]) { (results, handler) =>
        results.headOption match {
          case Some(FormHasErrors(_)) | Some(FormIsBlank(_)) => results
          case _                                             => bindPage(data, handler) +: results
        }
      }.head
    }
  }

  private def bindPage(data: JsObject, handler: FormHandler[_]) = {
    val boundHandler = handler.bind((data \\ handler.formName.entryName).headOption.getOrElse(Json.obj()))
    if (boundHandler.form.data.isEmpty && boundHandler.form.value.isEmpty) FormIsBlank(boundHandler)
    else if (boundHandler.form.hasErrors) FormHasErrors(boundHandler)
    else FormIsOk(boundHandler)
  }

  def post(formName: FormName, companiesHouseId: CompaniesHouseId): Action[Map[String, Seq[String]]] =
    companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) {
      implicit request =>
        val handler = pagedLongFormModel.handlerFor(formName)

        for {
          _ <- saveFormData(handler.formName, handler.bind.form)
          result <- handlePostFormPage(formName, request.companyDetail)
        } yield result
    }

  private def handlePostFormPage(formName: FormName, companyDetail: CompanyDetail)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Future[Result] = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formName).map {
      case FormHasErrors(handler) => BadRequest(page(title)(handler.renderPage(reportPageHeader, companyDetail)))
      case FormIsOk(handler)      => Redirect(handler.nextPageCall(companyDetail))
      case FormIsBlank(handler)   => Ok(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
    }
  }

  private def bindLongForm(implicit sessionId: SessionId): Future[Option[LongFormModel]] = {
    sessionService.get[JsObject](sessionId, formDataSessionKey).map {
      case None       => None
      case Some(data) =>
        for {
          ps <- emptyPaymentStatisticsForm.bind((data \\ PaymentStatistics.entryName).headOption.getOrElse(Json.obj())).value
          pt <- emptyPaymentTermsForm.bind((data \\ PaymentTerms.entryName).headOption.getOrElse(Json.obj())).value
          dr <- emptyDisputeResolutionForm.bind((data \\ DisputeResolution.entryName).headOption.getOrElse(Json.obj())).value
          oi <- emptyOtherInformationForm.bind((data \\ OtherInformation.entryName).headOption.getOrElse(Json.obj())).value
        } yield LongFormModel(ps.paymentStatistics, pt.paymentTerms, dr.disputeResolution, oi.otherInformation)
    }
  }


  def showReview(companiesHouseId: CompaniesHouseId): Action[AnyContent] = companyAuthAction(companiesHouseId).async { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)
    val action: Call = routes.ShortFormController.postReview(companiesHouseId)

    bindAllPages.flatMap {
      case FormHasErrors(handler) => Future.successful(Redirect(handler.pageCall(request.companyDetail)))
      case FormIsBlank(handler)   => Future.successful(Redirect(handler.pageCall(request.companyDetail)))
      case FormIsOk(handler)      =>
        val forms = for {
          reportingPeriod <- OptionT(loadFormData(emptyReportingPeriod, FormName.ReportingPeriod.entryName).map(_.value))
          longForm <- OptionT(bindLongForm)
        } yield (reportingPeriod, longForm)

        forms.value.map {
          case None          => ???
          case Some((r, lf)) =>
            val formGroups = ReviewPageData.formGroups(request.companyDetail.companyName, r, lf)
            Ok(page(title)(views.html.report.review(emptyReview, formGroups, action)))
        }
    }
  }
}

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
import config.{PageConfig, ServiceConfig}
import controllers.FormPageDefs._
import forms.report.Validations
import models.CompaniesHouseId
import org.scalactic.TripleEquals._
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.twirl.api.Html
import services._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.existentials

class MultiPageFormController @Inject()(
  validations: Validations,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  longFormPageModel: MultiPageFormPageModel
)(implicit val ec: ExecutionContext, messages: MessagesApi)
  extends Controller
    with PageHelper
    with FormSessionHelpers {

  import longFormPageModel._

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  private def reportPageHeader(companyDetail: CompanyDetail): Html = h1(s"Publish a report for:<br>${companyDetail.companyName}")

  implicit def sessionIdFromRequest(implicit request: CompanyAuthRequest[_]): SessionId = request.sessionId

  def show(formName: MultiPageFormName, companiesHouseId: CompaniesHouseId, change: Option[Boolean] = None): Action[AnyContent] = companyAuthAction(companiesHouseId).async { implicit request =>
    val companyDetail = request.companyDetail
    val back = backCrumb(backLink(formName, companiesHouseId, change))

    handleShowPage(formName, companyDetail, change.contains(true), back)
  }

  private[controllers] def handleShowPage(formName: MultiPageFormName, companyDetail: CompanyDetail, change: Boolean, crumb: Html)(implicit sessionId: SessionId, pageContext: PageContext) = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formHandlers, formName).map {
      case FormHasErrors(boundHandler) if boundHandler.formName !== formName => Redirect(boundHandler.callPage(companyDetail.companiesHouseId, change))
      case FormIsBlank(boundHandler) if boundHandler.formName !== formName   => Redirect(boundHandler.callPage(companyDetail.companiesHouseId, change))

      case FormHasErrors(boundHandler) => BadRequest(page(title)(crumb, boundHandler.renderPage(reportPageHeader(companyDetail), companyDetail.companiesHouseId, change)))
      // Form is blank, so the user hasn't filled it in yet. In this case we don't
      // want to show errors, so use the empty form handler for the formName
      case FormIsBlank(_) => Ok(page(title)(crumb, handlerFor(formName).renderPage(reportPageHeader(companyDetail), companyDetail.companiesHouseId, change)))

      case FormIsOk(handler, value) => Ok(page(title)(crumb, handler.renderPage(reportPageHeader(companyDetail), companyDetail.companiesHouseId, change)))
    }
  }

  //noinspection TypeAnnotation
  def post(formName: MultiPageFormName, companiesHouseId: CompaniesHouseId, change: Option[Boolean] = None) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val handler = handlerFor(formName)
    val back = backCrumb(backLink(formName, companiesHouseId, change))

    for {
      _ <- saveFormData(handler.formName, handler.bind.form)
      result <- handlePostFormPage(formName, request.companyDetail, change.contains(true), back)
    } yield result
  }

  private def handlePostFormPage(formName: MultiPageFormName, companyDetail: CompanyDetail, change: Boolean, crumb: Html)(implicit sessionId: SessionId, pageContext: PageContext): Future[Result] = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formHandlers, formName).map {
      case FormHasErrors(handler) => BadRequest(page(title)(crumb, handler.renderPage(reportPageHeader(companyDetail), companyDetail.companiesHouseId, change)))
      case FormIsBlank(handler)   => BadRequest(page(title)(crumb, handler.renderPage(reportPageHeader(companyDetail), companyDetail.companiesHouseId, change)))

      case FormIsOk(handler, value) => nextFormHandler(handler) match {
        case Some(nextHandler) if !change => Redirect(nextHandler.callPage(companyDetail.companiesHouseId, change))
        case _                            => Redirect(routes.MultiPageFormReviewController.showReview(companyDetail.companiesHouseId))
      }
    }
  }

  //noinspection TypeAnnotation
  private def backLink(formName: MultiPageFormName, companiesHouseId: CompaniesHouseId, change: Option[Boolean]): String = {
    if (change.contains(true)) routes.MultiPageFormReviewController.showReview(companiesHouseId).url
    else previousFormName(formName).map(handlerFor) match {
      case Some(previousHandler) => previousHandler.callPage(companiesHouseId, change = false).url
      case None                  => routes.ReportController.search(None, None, None).url
    }
  }
}

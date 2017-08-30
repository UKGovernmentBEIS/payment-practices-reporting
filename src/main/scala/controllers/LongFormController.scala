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
import controllers.FormPageModels._
import forms.report.Validations
import models.CompaniesHouseId
import org.scalactic.TripleEquals._
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.twirl.api.Html
import services._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.existentials

class LongFormController @Inject()(
  validations: Validations,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  longFormPageModel: LongFormPageModel
)(implicit val ec: ExecutionContext, messages: MessagesApi)
  extends Controller
    with PageHelper
    with FormSessionHelpers {

  import longFormPageModel._

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"
  private def reportPageHeader(companyDetail: CompanyDetail): Html = h1(s"Publish a report for:<br>${companyDetail.companyName}")

  implicit def sessionIdFromRequest(implicit request: CompanyAuthRequest[_]): SessionId = request.sessionId

  def show(formName: LongFormName, companiesHouseId: CompaniesHouseId): Action[AnyContent] = companyAuthAction(companiesHouseId).async { implicit request =>
    val companyDetail = request.companyDetail

    handleShowPage(formName, companyDetail)
  }

  private[controllers] def handleShowPage(formName: LongFormName, companyDetail: CompanyDetail)(implicit sessionId: SessionId, pageContext: PageContext) = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formHandlers, formName).map {
      case FormHasErrors(boundHandler) if boundHandler.formName !== formName => Redirect(boundHandler.callPage(companyDetail))
      case FormIsBlank(boundHandler) if boundHandler.formName !== formName   => Redirect(boundHandler.callPage(companyDetail))

      case FormHasErrors(boundHandler) => BadRequest(page(title)(boundHandler.renderPage(reportPageHeader(companyDetail), companyDetail)))
      // Form is blank, so the user hasn't filled it in yet. In this case we don't
      // want to show errors, so use the empty form handler for the formName
      case FormIsBlank(_) => Ok(page(title)(handlerFor(formName).renderPage(reportPageHeader(companyDetail), companyDetail)))

      case FormIsOk(handler, value) => Ok(page(title)(handler.renderPage(reportPageHeader(companyDetail), companyDetail)))
    }
  }

  //noinspection TypeAnnotation
  def post(formName: LongFormName, companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val handler = handlerFor(formName)

    for {
      _ <- saveFormData(handler.formName, handler.bind.form)
      result <- handlePostFormPage(formName, request.companyDetail)
    } yield result
  }

  private def handlePostFormPage(formName: LongFormName, companyDetail: CompanyDetail)(implicit sessionId: SessionId, pageContext: PageContext): Future[Result] = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formHandlers, formName).map {
      case FormHasErrors(handler) => BadRequest(page(title)(handler.renderPage(reportPageHeader(companyDetail), companyDetail)))
      case FormIsBlank(handler)   => BadRequest(page(title)(handler.renderPage(reportPageHeader(companyDetail), companyDetail)))

      case FormIsOk(handler, value) => nextFormHandler(handler) match {
        case Some(nextHandler) => Redirect(nextHandler.callPage(companyDetail))
        case None              => Redirect(routes.LongFormReviewController.showReview(companyDetail.companiesHouseId))
      }
    }
  }
}

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

import forms.report.{ReportFormModel, ReportReviewModel, Validations}
import models.CompaniesHouseId
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import services.CompaniesHouseAPI
import slicks.modules.ReportRepo
import utils.TimeSource

import scala.concurrent.{ExecutionContext, Future}

class ReportController @Inject()(companiesHouseAPI: CompaniesHouseAPI, reports: ReportRepo, timeSource: TimeSource)(implicit ec: ExecutionContext, messages: MessagesApi) extends Controller with PageHelper {

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

  def start(companiesHouseId: CompaniesHouseId) = Action { request =>
    Ok(page(home, views.html.report.signInInterstitial(companiesHouseId)))
  }

  val hasAccountChoice = Form(mapping("account" -> boolean)(identity)(b => Some(b)))

  def login(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    val next = hasAccountChoice.bindFromRequest().fold(
      errs => routes.ReportController.start(companiesHouseId),
      hasAccount =>
        if (hasAccount) routes.CoHoOAuthMockController.login(companiesHouseId)
        else routes.ReportController.code(companiesHouseId)
    )

    Redirect(next)
  }

  def code(companiesHouseId: CompaniesHouseId) = Action.async { request =>
    companiesHouseAPI.find(companiesHouseId).map {
      case Some(co) => Ok(page(home, views.html.report.companiesHouseOptions(co.company_name, companiesHouseId)))
      case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
    }
  }

  def codeOptions(companiesHouseId: CompaniesHouseId) = Action { request => ??? }

  def file(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>
    request.session.get("company_id") match {
      case Some(id) if id == companiesHouseId.id => companiesHouseAPI.find(companiesHouseId).map {
        case Some(co) => Ok(page(home, h1("Publish a report for company.name"), views.html.report.file(emptyReport, companiesHouseId, LocalDate.now(), df)))
        case None => BadRequest(s"Unknown company id ${companiesHouseId.id}")
      }
      case _ => Future.successful(Redirect(controllers.routes.ReportController.start(companiesHouseId)))
    }
  }

  def postForm(companiesHouseId: CompaniesHouseId) = Action { implicit request =>
    emptyReport.bindFromRequest().fold(
      errs => BadRequest(page(h1("Publish a report for company.name"), views.html.report.file(errs, companiesHouseId, LocalDate.now(), df))),
      report => {
        Ok(page(home, views.html.report.review(emptyReview, report, companiesHouseId, "<company name>", df, reportValidations.reportFormModel)))
      }
    )
  }

  def postReview(companiesHouseId: CompaniesHouseId) = Action(parse.urlFormEncoded) { implicit request =>
    println(request.body)
    // Re-capture the values for the report itself. In theory these values should always be valid
    // (as we only send the user to the review page if they are) but if somehow they aren't then
    // send them back to the report form.
    emptyReport.bindFromRequest().fold(
      errs => BadRequest(page(home, h1("Publish a report for company.name"), views.html.report.file(errs, companiesHouseId, LocalDate.now(), df))),
      report => {
        if (request.body.get("revise").isDefined) Ok(page(home, h1("Publish a report for company.name"), views.html.report.file(emptyReport.fill(report), companiesHouseId, LocalDate.now(), df)))
        else emptyReview.bindFromRequest().fold(
          errs => BadRequest(page(home, views.html.report.review(errs, report, companiesHouseId, "<company name>", df, reportValidations.reportFormModel))),
          confirmation =>
            if (confirmation.confirmed) ???
            else BadRequest(page(home, views.html.report.review(emptyReview.fill(confirmation), report, companiesHouseId, "<company name>", df, reportValidations.reportFormModel)))
        )
      }
    )
  }

}

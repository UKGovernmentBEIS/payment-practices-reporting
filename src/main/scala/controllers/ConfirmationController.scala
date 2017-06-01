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

import config.{PageConfig, ServiceConfig}
import models.ReportId
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import services._

import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(
                                  reports: ReportService,
                                  val serviceConfig: ServiceConfig,
                                  val pageConfig: PageConfig
                                )(implicit val ec: ExecutionContext, messages: MessagesApi) extends Controller with PageHelper {

  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"
  private def publishTitle(companyName: String) = s"Publish a report for $companyName"


  def showConfirmation(reportId: ReportId) = Action.async { implicit request =>
    reports.find(reportId).map {
      case Some(report) => Ok(
        page(s"You have published a report for ${report.companyName}")
        (home, pages.filingSuccess(reportId, report.confirmationEmailAddress, pageConfig.surveyMonkeyConfig)))
      case None => BadRequest(s"Could not find a report with id ${reportId.id}")
    }
  }
}

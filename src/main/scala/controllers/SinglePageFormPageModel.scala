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
import config.ServiceConfig
import controllers.FormPageDefs.{SinglePageFormHandler, SinglePageFormName}
import forms.report.{LongFormModel, ReportingPeriodFormModel, Validations}
import models.CompaniesHouseId
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.json.JsObject
import play.twirl.api.Html

import scala.concurrent.Future

class SinglePageFormPageModel @Inject()(validations: Validations, serviceConfig: ServiceConfig)(implicit messagesApi: MessagesApi)
  extends FormPageModel[SinglePageFormHandler[_], SinglePageFormName]
    with FormPageHelpers[SinglePageFormHandler[_], SinglePageFormName] {

  import FormPageDefs.SinglePageFormName
  import SinglePageFormName._
  import validations._
  import views.html.{report => pages}

  private val df = DateTimeFormat.forPattern("d MMMM YYYY")

  private val serviceStartDate = serviceConfig.startDate.getOrElse(ServiceConfig.defaultServiceStartDate)

  override def formNames: Seq[SinglePageFormName] = SinglePageFormName.values

  override def handlerFor(formName: SinglePageFormName): SinglePageFormHandler[_] = formName match {
    case ReportingPeriod =>
      FormHandler(
        ReportingPeriod,
        emptyReportingPeriod,
        (header: Html, companiesHouseId: CompaniesHouseId, change: Boolean, session: Option[JsObject]) => (form: Form[ReportingPeriodFormModel]) =>
          pages.reportingPeriod(header, form, companiesHouseId, df, serviceStartDate, if (change) Some(true) else None),
        (companiesHouseId: CompaniesHouseId, change: Boolean) =>
          routes.ReportingPeriodController.show(companiesHouseId, if (change) Some(true) else None)
      )

    case SinglePageForm =>
      FormHandler(
        SinglePageForm,
        emptyLongForm,
        (header: Html, companiesHouseId: CompaniesHouseId, change: Boolean, session: Option[JsObject]) => (form: Form[LongFormModel]) =>
          pages.longForm(header, form, false, companiesHouseId, df, serviceStartDate, if (change) Some(true) else None),
        (companiesHouseId: CompaniesHouseId, change: Boolean) =>
          routes.ShortFormController.show(companiesHouseId, if (change) Some(true) else None)
      )
  }
}

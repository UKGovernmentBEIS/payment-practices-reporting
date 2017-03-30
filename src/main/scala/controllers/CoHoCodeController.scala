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

import config.GoogleAnalyticsConfig
import controllers.CoHoCodeController.CodeOption.{Colleague, Register}
import models.CompaniesHouseId
import play.api.data.Forms.single
import play.api.data.{Form, FormError, Forms}
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.{Action, Controller, RequestHeader, Result}
import play.twirl.api.Html
import services.{CompanyAuthService, CompanySearchService, ReportService}
import utils.AdjustErrors

import scala.concurrent.{ExecutionContext, Future}

class CoHoCodeController @Inject()(
                                    companyAuth: CompanyAuthService,
                                    val companySearch: CompanySearchService,
                                    val reportService: ReportService,
                                    val googleAnalytics: GoogleAnalyticsConfig
                                  )(implicit val ec: ExecutionContext, messages: MessagesApi)
  extends Controller
    with PageHelper
    with SearchHelper
    with CompanyHelper {

  import CoHoCodeController._
  import views.html.{report => pages}

  private def codePage(companiesHouseId: CompaniesHouseId, form: Form[CodeOption] = emptyForm, foundResult: Html => Result = Ok(_))
                      (implicit messages: MessagesApi, rh: RequestHeader) =
    withCompany(companiesHouseId, foundResult) { co =>
      page("If you don't have a Companies House authentication code")(home, pages.companiesHouseOptions(co.companyName, companiesHouseId, form))
    }

  def code(companiesHouseId: CompaniesHouseId) = Action.async(implicit request => codePage(companiesHouseId))

  def codeOptions(companiesHouseId: CompaniesHouseId) = Action.async { implicit request =>
    def resultFor(codeOption: CodeOption): Future[Result] = Future {
      codeOption match {
        case Colleague => Redirect(routes.ReportController.colleague(companiesHouseId))
        case Register => Redirect(routes.ReportController.register(companiesHouseId))
      }
    }

    emptyForm.bindFromRequest().fold(errs => codePage(companiesHouseId, errs, BadRequest(_)), resultFor)
  }

}

object CoHoCodeController {

  import enumeratum.EnumEntry.Lowercase
  import enumeratum._
  import utils.EnumFormatter

  sealed trait CodeOption extends EnumEntry with Lowercase

  object CodeOption extends Enum[CodeOption] with EnumFormatter[CodeOption] {
    override def values = findValues

    case object Colleague extends CodeOption

    case object Register extends CodeOption

  }

  /**
    * Override any error from the EnumFormatter with a simple "need to choose
    * an option to continue" error.
    */
  val codeOptionMapping = AdjustErrors(Forms.of[CodeOption]) { (k, errs) =>
    if (errs.isEmpty) errs else Seq(FormError(k, "error.needchoicetocontinue"))
  }

  val emptyForm: Form[CodeOption] = Form(single("nextstep" -> codeOptionMapping))

}
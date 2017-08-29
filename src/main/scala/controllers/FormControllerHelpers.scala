package controllers

import actions.CompanyAuthRequest
import controllers.FormPageModels._
import forms.report.ReportingPeriodFormModel
import play.api.data.Form
import play.api.mvc.{Controller, Result}
import play.api.{Logger, UnexpectedException}
import services.SessionId

import scala.concurrent.Future
import scala.util.Random

trait FormControllerHelpers[T, N <: FormName] {
  self: Controller with FormSessionHelpers =>

  def formHandlers: Seq[FormHandler[_, N]]

  def bindMainForm(implicit sessionId: SessionId): Future[Option[T]]

  def bindReportingPeriod(implicit sessionId:SessionId):Future[Option[ReportingPeriodFormModel]]

  def emptyReportingPeriod: Form[ReportingPeriodFormModel]

  def handleBinding[A](request: CompanyAuthRequest[A], f: (CompanyAuthRequest[A], ReportingPeriodFormModel, T) => Future[Result]): Future[Result] = {
    implicit val req: CompanyAuthRequest[A] = request

    bindAllPages[N](formHandlers).flatMap {
      case FormHasErrors(handler) => Future.successful(Redirect(handler.pageCall(request.companyDetail)))
      case FormIsBlank(handler)   => Future.successful(Redirect(handler.pageCall(request.companyDetail)))
      case FormIsOk(handler)      =>
        val forms = for {
          reportingPeriod <- bindReportingPeriod
          longForm <- bindMainForm
        } yield (reportingPeriod, longForm)

        forms.flatMap {
          case (Some(r), Some(lf)) => f(request, r, lf)

          // The following cases should not happen - if one of them does it indicates
          // some kind of mismatch between the FormHandlers and the base form models
          case (_, _) =>
            val ref = Random.nextInt(1000000)
            Logger.error(s"Error reference $ref: The reporting period and/or main form did not bind correctly")
            throw UnexpectedException(Some(s"Error reference $ref"))
        }
    }
  }
}

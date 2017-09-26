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

import controllers.FormPageDefs.FormName
import enumeratum.EnumEntry.Uncapitalised
import enumeratum.{EnumEntry, PlayEnum}
import org.scalactic.TripleEquals._

import scala.language.existentials

object FormPageDefs {
  sealed trait FormName {
    def entryName: String
  }

  sealed trait MultiPageFormName extends FormName with EnumEntry with Uncapitalised
  sealed trait ShortFormName extends FormName with EnumEntry with Uncapitalised
  sealed trait SinglePageFormName extends FormName with EnumEntry with Uncapitalised

  type ShortFormHandler[T] = FormHandler[T, ShortFormName]
  type MultiPageFormHandler[T] = FormHandler[T, MultiPageFormName]
  type SinglePageFormHandler[T] = FormHandler[T, SinglePageFormName]

  object ShortFormName extends PlayEnum[ShortFormName] {
    //noinspection TypeAnnotation
    override def values = findValues

    case object ReportingPeriod extends ShortFormName
    case object ShortForm extends ShortFormName
  }

  object MultiPageFormName extends PlayEnum[MultiPageFormName] {
    //noinspection TypeAnnotation
    override def values = findValues

    /*
     * The order in which the form pages will be displayed to the user is defined
     * by the order in which these enumeration objects are defined.
     */
    case object ReportingPeriod extends MultiPageFormName
    case object PaymentStatistics extends MultiPageFormName
    case object PaymentTerms extends MultiPageFormName
    case object DisputeResolution extends MultiPageFormName
    case object OtherInformation extends MultiPageFormName
  }

  object SinglePageFormName extends PlayEnum[SinglePageFormName] {
    //noinspection TypeAnnotation
    override def values = findValues
    case object ReportingPeriod extends SinglePageFormName
    case object SinglePageForm extends SinglePageFormName
  }

  sealed trait FormStatus[T, N <: FormName]
  case class FormIsBlank[T, N <: FormName](formHandler: FormHandler[T, N]) extends FormStatus[T, N]
  case class FormHasErrors[T, N <: FormName](formHandler: FormHandler[T, N]) extends FormStatus[T, N]
  case class FormIsOk[T, N <: FormName](formHandler: FormHandler[T, N], value: T) extends FormStatus[T, N]

  type MultiPageFormStatus[T] = FormStatus[T, MultiPageFormName]
  type ShortFormStatus[T] = FormStatus[T, ShortFormName]
  type SinglePageFormStatus[T] = FormStatus[T, SinglePageFormName]
}

trait FormPageModel[H <: FormHandler[_, N], N <: FormName] {
  def formNames: Seq[N]
  def handlerFor(formName: N): H
}

trait FormPageHelpers[H <: FormHandler[_, N], N <: FormName] {
  self: FormPageModel[H, N] =>
  def formHandlers: Seq[H] = formNames.map(handlerFor)
  def nextFormName(formName: N): Option[N] = formNames.dropWhile(_ !== formName).drop(1).headOption
  def previousFormName(formName: N): Option[N] = formNames.takeWhile(_ !== formName).lastOption
  def nextFormHandler(handler: H): Option[H] = nextFormName(handler.formName).map(handlerFor)
  def previousFormHandler(handler: H): Option[H] = previousFormName(handler.formName).map(handlerFor)
}

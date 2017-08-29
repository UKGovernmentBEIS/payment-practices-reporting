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

import enumeratum.EnumEntry.Uncapitalised
import enumeratum.{EnumEntry, PlayEnum}
import scala.language.existentials

object FormPageModels {
  sealed trait FormName {
    def entryName:String
  }

  sealed trait LongFormName extends FormName with EnumEntry with Uncapitalised
  sealed trait ShortFormName extends FormName with EnumEntry with Uncapitalised

  type ShortFormHandler[T] = FormHandler[T, ShortFormName]

  object ShortFormName extends PlayEnum[ShortFormName] {
    //noinspection TypeAnnotation
    override def values = findValues

    case object ReportingPeriod extends ShortFormName
    case object ShortForm extends ShortFormName
  }

  type LongFormHandler[T] = FormHandler[T, LongFormName]

  object LongFormName extends PlayEnum[LongFormName] {
    //noinspection TypeAnnotation
    override def values = findValues

    case object ReportingPeriod extends LongFormName
    case object PaymentStatistics extends LongFormName
    case object PaymentTerms extends LongFormName
    case object DisputeResolution extends LongFormName
    case object OtherInformation extends LongFormName
  }

  sealed trait FormResult[N <: FormName]
  case class FormIsBlank[N <: FormName](formHandler: FormHandler[_, N]) extends FormResult[N]
  case class FormHasErrors[N <: FormName](formHandler: FormHandler[_, N]) extends FormResult[N]
  case class FormIsOk[N <: FormName](formHandler: FormHandler[_, N]) extends FormResult[N]

  type LongFormResult = FormResult[LongFormName]
  type ShortFormResult = FormResult[ShortFormName]
}

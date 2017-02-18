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

package utils

import play.api.data.validation.Constraint
import play.api.data.{FormError, Mapping}

/**
  * Wrap a `Mapping[T]` and override take a function `f` that transforms the list of errors
  */
case class AdjustErrors[T](wrapped: Mapping[T], additionalConstraints: Seq[Constraint[T]] = Nil)(f: (String, Seq[FormError]) => Seq[FormError])
  extends Mapping[T] {

  def bind(data: Map[String, String]): Either[Seq[FormError], T] = {
    wrapped.bind(data).right.flatMap(applyConstraints).left.map(f(wrapped.key, _))
  }

  val key = wrapped.key

  val mappings = wrapped.mappings

  override val format = wrapped.format

  val constraints: Seq[Constraint[T]] = additionalConstraints

  def unbind(value: T): Map[String, String] = wrapped.unbind(value)

  def unbindAndValidate(value: T): (Map[String, String], Seq[FormError]) = {
    val (data, errors) = wrapped.unbindAndValidate(value)
    (data, errors ++ collectErrors(value))
  }

  def withPrefix(prefix: String): Mapping[T] = copy(wrapped = wrapped.withPrefix(prefix))(f)

  def verifying(constraints: Constraint[T]*): Mapping[T] = copy(additionalConstraints = additionalConstraints ++ constraints)(f)
}

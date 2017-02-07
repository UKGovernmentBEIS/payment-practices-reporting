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

object Ordinals {
  private val wordOrdinals = Seq("zeroth", "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", "eleventh", "twelfth", "thirteenth")

  @throws[IllegalArgumentException]
  def forNumber(i: Int): String = {
    if (i < 0) throw new IllegalArgumentException("number must be non-negative")
    if (i < wordOrdinals.length && i >= 0) return wordOrdinals(i)
    if (i % 10 == 1) i + "st"
    else if (i % 10 == 2) i + "nd"
    else if (i % 10 == 3) i + "rd"
    else i + "th"
  }
}



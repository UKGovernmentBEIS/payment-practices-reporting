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

package config

case class SearchConfig(searchUrl: String)

object SearchConfig {
  private val searchPath = "search"

  val local = SearchConfig(
    s"http://localhost:9001/$searchPath"
  )

  def fromHostname(hostname: String): SearchConfig = {
    val Pattern = "beis-ppr-(.*)".r
    val pprBase = hostname match {
      case Pattern(environment) => s"https://beis-spp-$environment"
      case _ => "http://localhost:9001"
    }

    SearchConfig(s"$pprBase/$searchPath")
  }
}
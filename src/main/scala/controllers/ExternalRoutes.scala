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

import config.RoutesConfig

trait ExternalRouter {
  def search(): String
}

class ExternalRoutes (searchConfig: RoutesConfig) {
  val HerokuPattern = "beis-ppr-(.*)".r

  private val searchPath = "search"

  def apply(hostname: String) = new ExternalRouter {
    val root = hostname match {
      case HerokuPattern(environment) => s"https://beis-spp-$environment"
      case _ => searchConfig.searchHost
    }

    override def search(): String = s"$root/$searchPath"
  }
}
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

import config.RoutesConfig
import models.ReportId

import scala.util.matching.Regex

trait ExternalRouter {
  def root: String
  def search(): String
  def download(): String
  def report(reportId: ReportId): String
}

class ExternalRoutes(routesConfig: RoutesConfig) {
  val HerokuPattern: Regex = "beis-ppr-(.*)".r

  private val searchPath   = "search"
  private val downloadPath = "export"

  def apply(requestHostname: String): ExternalRouter = new ExternalRouter {
    override val root: String = routesConfig.searchHost match {
      case Some(hostname) => s"https://$hostname"
      case None           => requestHostname match {
        case HerokuPattern(environment) => s"https://beis-spp-$environment"
        case _                          => s"http://localhost:9001"
      }
    }

    override def search(): String = s"$root/$searchPath"

    override def download(): String = s"$root/$downloadPath"

    override def report(reportId: ReportId): String = s"$root/report/${reportId.id}"
  }
}
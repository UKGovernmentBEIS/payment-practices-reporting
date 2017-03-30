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

import akka.stream.scaladsl.{Concat, Source}
import akka.util.ByteString
import config.GoogleAnalyticsConfig
import org.joda.time.LocalDate
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}
import services.{FiledReport, ReportService}

class DownloadController @Inject()(reportRepo: ReportService, val googleAnalytics: GoogleAnalyticsConfig) extends Controller with PageHelper {

  def show = Action { implicit request =>
    Ok(page("Export data for published reports")(home, views.html.download.accessData()))
  }

  def export = Action { implicit request =>
    val disposition = ("Content-Disposition", "attachment;filename=payment-practices.csv")

    val publisher = reportRepo.list(LocalDate.now().minusMonths(24))

    val headerSource = Source.single(ReportCSV.columns.map(_._1).mkString(","))
    val rowSource = Source.fromPublisher(publisher).map(toCsv)
    val csvSource = Source.combine[String, String](headerSource, rowSource)(_ => Concat()).map(ByteString(_))

    val entity = HttpEntity.Streamed(csvSource, None, Some("text/csv"))
    Result(ResponseHeader(OK, Map()), entity).withHeaders(disposition)
  }

  def toCsv(row: FiledReport): String = "\n" + ReportCSV.columns.map(_._2(row).s).mkString(",")
}

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

package database

import dbrows.ReportRow
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, OptionValues, WordSpec}
import services.Report

import scala.concurrent.ExecutionContext.Implicits.global


class ReportTableSpec extends WordSpec with Matchers with H2TestDatabases with GeneratorDrivenPropertyChecks with OptionValues {

  import generators.ReportModelGenerators._
  import modules._
  import modules.profile.api._

  "find" should {
    "return None if report is not found" in {
      reportDatabase.find(ReportId(1)).futureValue shouldBe None
    }

    "return a report when it exists with a known id" in {
      val row = ReportRow(ReportId(-1), "company name", CompaniesHouseId("12345"), new LocalDate(), "approved by", "foo@bard.com", new LocalDate(), new LocalDate(), None)

      val f = for {
        id <- db.run(reportTable.returning(reportTable.map(_.id)) += row)
        result <- reportDatabase.find(id)
        report = Report((row.copy(id), None))
      } yield {
        result shouldBe Some(report)
      }

      f.futureValue
    }
  }

  "createShortReport" should {
    "create report and generate a confirmation" in {
      forAll(genCompanyDetails, genShortFormModel, genReportingPeriodFormModel) { (cd, sf, rp) =>
        val f = for {
          reportId <- reportDatabase.createShortReport(cd, rp, sf, "confirmed by", "foo@bar.com", r => r.id.toString)
          report <- reportDatabase.find(reportId)
          cd <- db.run(contractDetailsTable.filter(_.reportId === reportId).result.headOption)
          confirmation <- db.run(confirmationPendingTable.filter(_.reportId === reportId).result.headOption)
        } yield {
          cd shouldBe None
          confirmation shouldBe defined
        }

        f.futureValue
      }
    }
  }

  "createLongReport" should {
    "create report and generate a confirmation" in {
      forAll(genCompanyDetails, genLongFormModel, genReportingPeriodFormModel) { (cd, lf, rp) =>
        val f = for {
          reportId <- reportDatabase.createLongReport(cd, rp, lf, "confirmed by", "foo@bar.com", r => r.id.toString)
          report <- reportDatabase.find(reportId)
          cd <- db.run(contractDetailsTable.filter(_.reportId === reportId).result.headOption)
          confirmation <- db.run(confirmationPendingTable.filter(_.reportId === reportId).result.headOption)
        } yield {
          cd shouldBe defined
          confirmation shouldBe defined
        }

        f.futureValue
      }
    }
  }
}

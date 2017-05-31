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

package slicks

import slick.jdbc.PostgresProfile
import slicks.modules.{ConfirmationModule, CoreModule, ReportModule, SessionModule}


object GenerateSQL
  extends ReportModule
    with ConfirmationModule
    with SessionModule
    with CoreModule {

  override val profile = PostgresProfile

  import profile.api._

  val schema =
    shortFormTable.schema ++
      longFormTable.schema ++
      confirmationPendingTable.schema ++
      confirmationSentTable.schema ++
      confirmationFailedTable.schema ++
      sessionTable.schema


  def main(args: Array[String]): Unit = {
    println("# --- !Ups")
    schema.createStatements.foreach(s => println(s"$s;"))
    println("")
    println("# --- !Downs")
    schema.dropStatements.foreach(s => println(s"$s;"))
  }

}

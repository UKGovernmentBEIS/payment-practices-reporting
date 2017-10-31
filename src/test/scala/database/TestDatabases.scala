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

import com.typesafe.config.ConfigFactory
import org.scalatest.{AsyncTestSuite, BeforeAndAfterAll, TestSuite}
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.jdbc.{JdbcBackend, JdbcProfile}
import slicks.repos.ReportTable
import slicks.{AllModules, EvalDB}
import utils.FutureHelpers

import scala.concurrent.Future
import scala.util.Try

trait H2DBConfig {
  self: TestDatabases =>
  override lazy val dbConfig = DatabaseConfig.forConfig[JdbcProfile](
    "h2",
    ConfigFactory.parseString(
      s"""
        h2 {
          profile = "slick.jdbc.H2Profile$$"
          db {
            connectionPool = disabled
            driver = "org.h2.Driver"
            url = "jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1"
          }
        }
        """
    )
  )
}

trait H2TestDatabases extends TestDatabases with BeforeAndAfterAll with FutureHelpers with H2DBConfig {
  self: TestSuite =>

  override def beforeAll(): Unit = {
    super.beforeAll()
    dropCreate()
  }


  override def afterAll(): Unit = {
    super.afterAll()
    dropClose()
  }

}

trait TestDatabases extends FutureHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  val dbName = s"${getClass.getSimpleName}-${java.util.UUID.randomUUID.toString}"

  def dbConfig: DatabaseConfig[JdbcProfile]

  def dbConfigProvider: DatabaseConfigProvider = new DatabaseConfigProvider {
    override def get[P <: BasicProfile] = dbConfig.asInstanceOf[DatabaseConfig[P]]
  }

  lazy val modules: AllModules = new AllModules(dbConfig.profile)

  import modules.profile.api._
  import modules.schema

  lazy val db: JdbcBackend#DatabaseDef = dbConfig.db

  implicit lazy val evalDB: EvalDB = new EvalDB(dbConfig) // we have to import from here and not profile.api !!

  implicit class DBIOOps[A](dbio: DBIO[A]) {
    def run: Future[A] =
      db.run(dbio)
  }

  def dropCreate(): Unit = {
    Try(await(db.run(schema.drop)))
    await(db.run(schema.create))
  }

  def dropClose(): Unit = {
    await(db.run(schema.drop))
    db.close()
  }


  //  implicit val dbioMonad: Monad[DBIO] = clearingplus.dbioMonadError

  object reportDatabase extends ReportTable(dbConfigProvider)
  //
  //  object schoolDatabase extends SlickSchoolDatabase(modules)
}

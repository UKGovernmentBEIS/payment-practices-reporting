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

package slicks.helpers

import play.api.libs.json.{JsObject, Json}
import slick.jdbc.JdbcProfile

trait PlayJsonMappers {
  protected val profile: JdbcProfile

  import profile.api._

  implicit lazy val playJsObjectTypeMapper: BaseColumnType[JsObject] =
    MappedColumnType.base[JsObject, String](Json.stringify, Json.parse(_).as[JsObject])
}


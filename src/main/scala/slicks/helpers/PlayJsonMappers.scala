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

import com.github.tminglei.slickpg.{ExPostgresDriver, PgPlayJsonSupport}
import play.api.libs.json.{JsArray, JsObject, Json}
import slick.jdbc.JdbcType

trait PlayJsonMappers {
  self: ExPostgresDriver with PgPlayJsonSupport =>

  implicit val playJsObjectTypeMapper: JdbcType[JsObject] =
    new GenericJdbcType[JsObject](
      pgjson,
      (v) => Json.parse(v).as[JsObject],
      (v) => Json.stringify(v),
      zero = JsObject(Seq()),
      hasLiteralForm = false
    )

  implicit val playJsArrayTypeMapper: JdbcType[JsArray] =
    new GenericJdbcType[JsArray](
      pgjson,
      (v) => Json.parse(v).as[JsArray],
      (v) => Json.stringify(v),
      zero = JsArray(),
      hasLiteralForm = false
    )
}


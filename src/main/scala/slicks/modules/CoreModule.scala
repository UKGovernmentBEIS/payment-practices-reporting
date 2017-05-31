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

package slicks.modules

import java.sql.Timestamp

import org.joda.time.{LocalDate, LocalDateTime}
import slick.jdbc.JdbcProfile
import slicks.helpers.PlayJsonMappers

trait CoreModule extends PlayJsonMappers {

  protected val profile: JdbcProfile

  import profile.api._

  def localDateTimeToTimestamp(dt: LocalDateTime): Timestamp =
    new Timestamp(dt.toDateTime.getMillis)

  def timestampToLocalDateTime(ts: Timestamp): LocalDateTime =
    new LocalDateTime(ts.getTime)

  def localDateToTimestamp(dt: LocalDate): Timestamp =
    new Timestamp(dt.toDateTimeAtStartOfDay.getMillis)

  def timestampToLocalDate(ts: Timestamp): LocalDate =
    new LocalDateTime(ts.getTime).toLocalDate

  // TODO: Figure out if this is the right way to be handling dates
  // TODO: This is serializing with local time zone. Serialize with UTC instead to avoid DST errors:
  implicit lazy val localDateTimeColumnType: BaseColumnType[LocalDateTime] =
  MappedColumnType.base[LocalDateTime, Timestamp](localDateTimeToTimestamp, timestampToLocalDateTime)

  implicit lazy val localDateColumnType: BaseColumnType[LocalDate] =
    MappedColumnType.base[LocalDate, Timestamp](localDateToTimestamp, timestampToLocalDate)

}

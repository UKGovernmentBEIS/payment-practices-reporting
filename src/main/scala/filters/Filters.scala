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

package filters

import javax.inject.Inject

import play.api.http.DefaultHttpFilters

/**
  * This provides the wiring for the application's filters and is used in
  * the play configuration.
  */
class Filters @Inject()(
                         tls: TLSFilter,
                         log: LoggingFilter,
                         rest: RestErrorFilter,
                         securityHeadersFilter: SecurityHeadersFilter
                       ) extends DefaultHttpFilters(log, tls, securityHeadersFilter)
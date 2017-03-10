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

package dbrows

import models.ReportId
import org.joda.time.LocalDateTime

case class ConfirmationPendingRow(
                                   reportId: ReportId,
                                   emailAddress: String,
                                   url: String,
                                   retryCount: Int,
                                   lastErrorState: Option[Int],
                                   lastErrorText: Option[String],
                                   lockedAt: Option[LocalDateTime]
                                 )

case class ConfirmationSentRow(
                                reportId: ReportId,
                                emailAddress: String,
                                emailBody: String,
                                notificationId: String,
                                sentAt: LocalDateTime
                              )

case class ConfirmationFailedRow(
                                  reportId: ReportId,
                                  emailAddress: String,
                                  errorStatus: Int,
                                  errorText: String,
                                  failedAt: LocalDateTime
                                )
package services

import org.joda.time.LocalDateTime

case class OAuthToken(accessToken: String, accessTokenExpiry: LocalDateTime, refreshToken: String) {
  def isExpired = accessTokenExpiry.isAfter(LocalDateTime.now)
}

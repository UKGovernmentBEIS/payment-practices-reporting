package services.companiesHouse

import javax.inject.Inject

import com.wellfactored.playbindings.ValueClassReads
import config.AppConfig
import models.CompaniesHouseId
import org.scalactic.TripleEquals._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import services._

import scala.concurrent.{ExecutionContext, Future}

class CompaniesHouseAuth @Inject()(val ws: WSClient, oAuth2Service: OAuth2Service, appConfig: AppConfig)(implicit val ec: ExecutionContext)
  extends RestService
    with CompanyAuthService
    with ValueClassReads {

  private def bearerAuth(oAuthToken: OAuthToken) = s"Bearer ${oAuthToken.accessToken}"

  def targetScope(companiesHouseId: CompaniesHouseId): String = s"https://api.companieshouse.gov.uk/company/${companiesHouseId.id}"

  case class VerifyResult(scope: String)

  override def isInScope(companiesHouseId: CompaniesHouseId, oAuthToken: OAuthToken): Future[Boolean] = {
    implicit val verifyReads = Json.reads[VerifyResult]
    val url = "https://account.companieshouse.gov.uk/oauth2/verify"
    get[VerifyResult](url, bearerAuth(oAuthToken)).map(_.scope === targetScope(companiesHouseId))
  }

  case class Email(email: String)

  implicit val emailReads = Json.reads[Email]

  override def emailAddress(token: OAuthToken): Future[Option[String]] = {
    val url = "https://account.companieshouse.gov.uk/user/profile"
    getOpt[Email](url, bearerAuth(token)).map(_.map(_.email))
  }
}

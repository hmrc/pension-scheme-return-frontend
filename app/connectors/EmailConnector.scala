/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import utils.WithName
import uk.gov.hmrc.http.HttpReads.Implicits._
import com.google.inject.Inject
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import config.FrontendAppConfig
import models.SendEmailRequest
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.Logger
import play.api.libs.ws.JsonBodyWritables.given
import play.api.libs.json.Json
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

import java.nio.charset.StandardCharsets
import javax.inject.Singleton
import java.net.URLEncoder

sealed trait EmailStatus

case object EmailSent extends WithName("EmailSent") with EmailStatus

case object EmailNotSent extends WithName("EmailNotSent") with EmailStatus

@Singleton
class EmailConnector @Inject() (
  appConfig: FrontendAppConfig,
  http: HttpClientV2,
  crypto: ApplicationCrypto
) {

  private val logger = Logger(classOf[EmailConnector])

  private def callBackUrl(
    psaOrPsp: String,
    requestId: String,
    psaOrPspId: String,
    pstr: String,
    email: String,
    reportVersion: String,
    schemeName: String,
    taxYear: String,
    userName: String
  ): String = {
    val callbackUrl = appConfig.eventReportingEmailCallback(
      psaOrPsp,
      requestId,
      encrypt(email),
      encrypt(psaOrPspId),
      encrypt(pstr),
      reportVersion,
      encrypt(schemeName),
      taxYear,
      encrypt(userName)
    )
    logger.info(s"Callback URL: $callbackUrl")
    callbackUrl
  }

  private def encrypt(value: String): String =
    URLEncoder.encode(
      crypto.QueryParameterCrypto.encrypt(PlainText(value)).value,
      StandardCharsets.UTF_8.toString
    )

  // scalastyle:off parameter.number
  def sendEmail(
    psaOrPsp: String,
    requestId: String,
    psaOrPspId: String,
    pstr: String,
    emailAddress: String,
    templateId: String,
    templateParams: Map[String, String],
    reportVersion: String,
    schemeName: String,
    taxYear: String,
    userName: String
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[EmailStatus] = {
    val emailServiceUrl = s"${appConfig.emailApiUrl}/hmrc/email"

    val sendEmailReq = SendEmailRequest(
      List(emailAddress),
      templateId,
      templateParams,
      appConfig.emailSendForce,
      callBackUrl(psaOrPsp, requestId, psaOrPspId, pstr, emailAddress, reportVersion, schemeName, taxYear, userName)
    )
    val jsonData = Json.toJson(sendEmailReq)

    http
      .post(url"$emailServiceUrl")
      .withBody(jsonData)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case ACCEPTED =>
            logger.debug(s"Email sent successfully")
            EmailSent
          case status =>
            logger.warn(s"Sending Email failed with response status $status")
            EmailNotSent
        }
      }
      .recoverWith { t =>
        logger.warn("Unable to connect to Email Service", t)
        Future.successful(EmailNotSent)
      }
  }
}

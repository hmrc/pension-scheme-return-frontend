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
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

import java.nio.charset.StandardCharsets
import java.net.URLEncoder

sealed trait EmailStatus

case object EmailSent extends WithName("EmailSent") with EmailStatus

case object EmailNotSent extends WithName("EmailNotSent") with EmailStatus

class EmailConnector @Inject()(
  appConfig: FrontendAppConfig,
  http: HttpClient,
  crypto: ApplicationCrypto
) {

  private val logger = Logger(classOf[EmailConnector])

  private def callBackUrl(
    psaOrPsp: String,
    requestId: String,
    psaOrPspId: String,
    pstr: String,
    email: String,
    reportVersion: String
  ): String = {
    val encryptedPsaOrPspId = URLEncoder.encode(
      crypto.QueryParameterCrypto.encrypt(PlainText(psaOrPspId)).value,
      StandardCharsets.UTF_8.toString
    )
    val encryptedPstr =
      URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pstr)).value, StandardCharsets.UTF_8.toString)
    val encryptedEmail =
      URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(email)).value, StandardCharsets.UTF_8.toString)

    val callbackUrl = appConfig.eventReportingEmailCallback(
      psaOrPsp,
      requestId,
      encryptedEmail,
      encryptedPsaOrPspId,
      encryptedPstr,
      reportVersion
    )
    logger.info(s"Callback URL: $callbackUrl")
    callbackUrl
  }

  //scalastyle:off parameter.number
  def sendEmail(
    psaOrPsp: String,
    requestId: String,
    psaOrPspId: String,
    pstr: String,
    emailAddress: String,
    templateId: String,
    templateParams: Map[String, String],
    reportVersion: String
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[EmailStatus] = {
    val emailServiceUrl = s"${appConfig.emailApiUrl}/hmrc/email"

    val sendEmailReq = SendEmailRequest(
      List(emailAddress),
      templateId,
      templateParams,
      appConfig.emailSendForce,
      callBackUrl(psaOrPsp, requestId, psaOrPspId, pstr, emailAddress, reportVersion)
    )
    val jsonData = Json.toJson(sendEmailReq)

    http
      .POST[JsValue, HttpResponse](emailServiceUrl, jsonData)
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
      .recoverWith(logExceptions)
  }

  private def logExceptions: PartialFunction[Throwable, Future[EmailStatus]] = {
    case t: Throwable =>
      logger.warn("Unable to connect to Email Service", t)
      Future.successful(EmailNotSent)
  }
}

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

package config

import play.api.mvc.RequestHeader
import com.google.inject.{Inject, Singleton}
import models.SchemeId.Srn
import play.api.Configuration
import play.api.i18n.Lang

import scala.concurrent.duration.Duration

import java.time.LocalDate
import java.net.URLEncoder

@Singleton
class FrontendAppConfig @Inject() (config: Configuration) { self =>

  val host: String = config.get[String]("host")
  val appName: String = config.get[String]("appName")

  private val contactFormServiceIdentifier = config.get[String]("contact-frontend.serviceId")
  private val betaFeedbackUrl = config.get[String]("microservice.services.contact-frontend.beta-feedback-url")
  private val reportProblemUrl = config.get[String]("microservice.services.contact-frontend.report-problem-url")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$betaFeedbackUrl?service=$contactFormServiceIdentifier&backUrl=${URLEncoder.encode(host + request.uri, "UTF-8")}"

  def reportAProblemUrl: String = s"$reportProblemUrl?service=$contactFormServiceIdentifier"

  def languageMap: Map[String, Lang] = Map("en" -> Lang("en"))

  val timeout: Int = config.get[Int]("timeout-dialog.timeout")
  val countdown: Int = config.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Int = config.get[Int]("mongodb.timeToLiveInSeconds")
  val uploadTtl: Int = config.get[Int]("mongodb.upload.timeToLiveInSeconds")

  val pensionsAdministrator: Service = config.get[Service]("microservice.services.pensionAdministrator")
  val pensionsScheme: Service = config.get[Service]("microservice.services.pensionsScheme")
  val pensionSchemeReturn: Service = config.get[Service]("microservice.services.pensionSchemeReturn")
  private val pensionSchemeReturnFrontend: Service =
    config.get[Service]("microservice.services.pensionSchemeReturnFrontend")

  val addressLookup: Service = config.get[Service]("microservice.services.address-lookup")

  val upscan: Service = config.get[Service]("microservice.services.upscan")
  val upscanMaxFileSize: Int = config.get[Int]("microservice.services.upscan.maxFileSize")
  val upscanMaxFileSizeMB: String = s"${upscanMaxFileSize}MB"
  val upscanCallbackEndpoint: String =
    s"${pensionSchemeReturnFrontend.baseUrl}${config.get[String]("urls.upscanCallback")}"

  private val emailService: Service = config.get[Service]("microservice.services.email")
  val emailApiUrl: String = emailService.baseUrl
  val emailSendForce: Boolean = config.getOptional[Boolean]("email.force").getOrElse(false)
  val fileReturnTemplateId: String = config.get[String]("email.fileReturnTemplateId")
  val allowedStartDateRange: LocalDate = LocalDate.parse(config.get[String]("schemeStartDate"))
  val prePopulationEnabled: Boolean = config.getOptional[Boolean]("prePopulationEnabled").getOrElse(false)
  val lockingEnabled: Boolean = config.getOptional[Boolean]("lockingEnabled").getOrElse(false)
  val ifsTimeout: Duration = config.get[Duration]("ifs.timeout")

  def eventReportingEmailCallback(
    psaOrPsp: String,
    requestId: String,
    encryptedEmail: String,
    encryptedPsaId: String,
    encryptedPstr: String,
    reportVersion: String,
    encryptedSchemeName: String,
    taxYear: String,
    encryptedUserName: String
  ): String =
    s"${pensionSchemeReturn.baseUrl}${config
        .get[String](path = "urls.emailCallback")
        .format(
          psaOrPsp,
          requestId,
          encryptedEmail,
          encryptedPsaId,
          encryptedPstr,
          reportVersion,
          encryptedSchemeName,
          taxYear,
          encryptedUserName
        )}"

  object urls {
    val loginUrl: String = config.get[String]("urls.login")
    val loginContinueUrl: String = config.get[String]("urls.loginContinue")
    val signOutSurvey: String = config.get[String]("urls.signOutSurvey")
    val signOutNoSurveyUrl: String = config.get[String]("urls.signOutNoSurvey")
    val pensionSchemeEnquiry: String = config.get[String]("urls.pensionSchemeEnquiry")
    val incomeTaxAct: String = config.get[String]("urls.incomeTaxAct")
    val unauthorisedSurrenders: String = config.get[String]("urls.unauthorisedSurrenders")
    val tangibleMoveableProperty: String = config.get[String]("urls.tangibleMoveableProperty")
    val sippBaseUrl: String = config.get[String]("urls.sippBaseUrl")
    val sippStartJourney: String = config.get[String]("urls.sippStartJourney")
    val sippContinueJourney: String = config.get[String]("urls.sippContinueJourney")
    val sippViewAndChange: String = config.get[String]("urls.sippViewAndChange")

    object managePensionsSchemes {
      val baseUrl: String = config.get[String]("urls.manage-pension-schemes.baseUrl")
      val registerUrl: String = baseUrl + config.get[String]("urls.manage-pension-schemes.register")
      val adminOrPractitionerUrl: String =
        baseUrl + config.get[String]("urls.manage-pension-schemes.adminOrPractitioner")
      val contactHmrc: String = baseUrl + config.get[String]("urls.manage-pension-schemes.contactHmrc")
      val cannotAccessDeregistered: String =
        baseUrl + config.get[String]("urls.manage-pension-schemes.cannotAccessDeregistered")
      val overview: String = baseUrl + config.get[String]("urls.manage-pension-schemes.overview")

      def schemeSummaryDashboard(srn: Srn): String =
        s"$baseUrl${config
            .get[String](path = "urls.manage-pension-schemes.schemeSummaryDashboard")
            .format(
              srn.value
            )}"

      def schemeSummaryPSPDashboard(srn: Srn): String =
        s"$baseUrl${config
            .get[String](path = "urls.manage-pension-schemes.schemeSummaryPSPDashboard")
            .format(
              srn.value
            )}"
    }

    object pensionAdministrator {
      val baseUrl: String = config.get[String]("urls.pension-administrator.baseUrl")
      val updateContactDetails: String = baseUrl + config.get[String]("urls.pension-administrator.updateContactDetails")
    }

    object pensionPractitioner {
      val baseUrl: String = config.get[String]("urls.pension-practitioner.baseUrl")
      val updateContactDetails: String = baseUrl + config.get[String]("urls.pension-administrator.updateContactDetails")
    }

    object upscan {
      val initiate: String = self.upscan.baseUrl + config.get[String]("urls.upscan.initiate")
      val successEndpoint: String = config.get[String]("urls.upscan.success-endpoint")
      val failureEndpoint: String = config.get[String]("urls.upscan.failure-endpoint")
    }
  }
}

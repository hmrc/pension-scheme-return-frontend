/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl

@Singleton
class FrontendAppConfig @Inject()(config: Configuration) { self =>

  val host: String = config.get[String]("host")
  val appName: String = config.get[String]("appName")

  private val contactHost = config.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "pension-scheme-return-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  private val exitSurveyBaseUrl: String = config.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/pension-scheme-return-frontend"

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int = config.get[Int]("timeout-dialog.timeout")
  val countdown: Int = config.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Int = config.get[Int]("mongodb.timeToLiveInSeconds")
  val uploadTtl: Int = config.get[Int]("mongodb.upload.timeToLiveInSeconds")

  val pensionsAdministrator: Service = config.get[Service]("microservice.services.pensionAdministrator")
  val pensionsScheme: Service = config.get[Service]("microservice.services.pensionsScheme")

  val upscan: Service = config.get[Service]("microservice.services.upscan")
  val secureUpscanCallBack: Boolean = config.getOptional[Boolean]("microservice.services.upscan.secure").getOrElse(true)

  object features {
    val welshTranslation: Boolean = config.get[Boolean]("features.welsh-translation")
  }

  object urls {
    val loginUrl: String = config.get[String]("urls.login")
    val loginContinueUrl: String = config.get[String]("urls.loginContinue")
    val signOutUrl: String = config.get[String]("urls.signOut")
    val pensionSchemeEnquiry: String = config.get[String]("urls.pensionSchemeEnquiry")

    object managePensionsSchemes {
      val baseUrl: String = config.get[String]("urls.manage-pension-schemes.baseUrl")
      val registerUrl: String = baseUrl + config.get[String]("urls.manage-pension-schemes.register")
      val adminOrPractitionerUrl: String =
        baseUrl + config.get[String]("urls.manage-pension-schemes.adminOrPractitioner")
      val contactHmrc: String = baseUrl + config.get[String]("urls.manage-pension-schemes.contactHmrc")
      val cannotAccessDeregistered: String =
        baseUrl + config.get[String]("urls.manage-pension-schemes.cannotAccessDeregistered")
      val dashboard = baseUrl + config.get[String]("urls.manage-pension-schemes.overview")
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
    }
  }
}

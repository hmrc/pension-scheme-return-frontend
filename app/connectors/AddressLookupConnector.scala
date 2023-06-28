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

package connectors

import config.FrontendAppConfig
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.util.{Failure, Success, Try}

class AddressLookupConnector @Inject()(http: HttpClient, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) {

  private val initUrl = appConfig.addressLookupFrontend.baseUrl + "/api/init"

  def init(continueUrl: String)(implicit hc: HeaderCarrier): Future[String] =
    http
      .POST[JsObject, HttpResponse](
        initUrl,
        initConfig(continueUrl),
        headers = List("Content-Type" -> "application/json")
      )
      .flatMap { response =>
        Future.fromTry(
          response.headers
            .get("location")
            .flatMap(_.headOption)
            .fold[Try[String]](Failure(new RuntimeException("location header expected")))(Success(_))
        )
      }

  private def initConfig(continueUrl: String) = Json.obj(
    "version" -> 2,
    "options" -> Json.obj(
      "continueUrl" -> continueUrl,
      "phaseFeedbackLink" -> "/help/alpha",
      "allowedCountryCodes" -> Json.arr(
        "GB"
      ),
      "ukMode" -> true,
      "alphaPhase" -> true,
      "showPhaseBanner" -> true,
      "disableTranslations" -> true,
      "showBackButtons" -> true,
      "includeHMRCBranding" -> false
    ),
    "labels" -> Json.obj(
      "en" -> Json.obj(
        "appLevelLabels" -> Json.obj(
          "navTitle" -> "Pension Scheme Return"
        )
      )
    )
  )

}

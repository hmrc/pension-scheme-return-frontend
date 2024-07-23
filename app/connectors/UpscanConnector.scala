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

import uk.gov.hmrc.http.HttpReads.Implicits._
import config.FrontendAppConfig
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import models._
import play.mvc.Http.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class UpscanConnector @Inject()(httpClientV2: HttpClientV2, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) {

  private val maxFileSizeMB = appConfig.upscanMaxFileSize

  private val header = HeaderNames.CONTENT_TYPE -> "application/json"

  def initiate(
    callBackUrl: String,
    successRedirectUrl: String,
    failureRedirectUrl: String
  )(implicit headerCarrier: HeaderCarrier): Future[UpscanInitiateResponse] = {
    val request = UpscanInitiateRequest(
      callbackUrl = callBackUrl,
      successRedirect = Some(successRedirectUrl),
      errorRedirect = Some(failureRedirectUrl),
      maximumFileSize = Some(maxFileSizeMB * (1024 * 1024))
    )

    httpClientV2
      .post(url"${appConfig.urls.upscan.initiate}")
      .withBody(Json.toJson(request))
      .setHeader(header)
      .execute[PreparedUpload]
      .map { response =>
        val fileReference = UpscanFileReference(response.reference.reference)
        val postTarget = response.uploadRequest.href
        val formFields = response.uploadRequest.fields
        UpscanInitiateResponse(fileReference, postTarget, formFields)
      }
  }

  def download(downloadUrl: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClientV2
      .get(url"$downloadUrl")
      .execute[HttpResponse]
}

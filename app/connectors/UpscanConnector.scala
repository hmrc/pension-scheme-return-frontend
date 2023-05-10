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
import models.{PreparedUpload, UpscanFileReference, UpscanInitiateRequest, UpscanInitiateResponse}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpscanConnector @Inject()(httpClient: HttpClient, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) {

  private val maxFileSizeMB = 100

  private val headers = Map(
    HeaderNames.CONTENT_TYPE -> "application/json"
  )

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

    httpClient
      .POST[UpscanInitiateRequest, PreparedUpload](appConfig.urls.upscan.initiate, request, headers.toSeq)
      .map { response =>
        val fileReference = UpscanFileReference(response.reference.reference)
        val postTarget = response.uploadRequest.href
        val formFields = response.uploadRequest.fields
        UpscanInitiateResponse(fileReference, postTarget, formFields)
      }
  }

  def download(downloadUrl: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient.GET[HttpResponse](appConfig.upscan.baseUrl + downloadUrl)
}

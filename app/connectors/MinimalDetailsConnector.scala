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

import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import config.{Constants, FrontendAppConfig}
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import connectors.MinimalDetailsError.{DelimitedAdmin, DetailsNotFound}
import play.api.Logging
import models.MinimalDetails
import uk.gov.hmrc.http.client.HttpClientV2
import utils.FutureUtils.FutureOps
import play.api.http.Status.{FORBIDDEN, NOT_FOUND}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class MinimalDetailsConnector @Inject() (appConfig: FrontendAppConfig, http: HttpClientV2) extends Logging {

  private val url = s"${appConfig.pensionsAdministrator}/pension-administrator/get-minimal-details-self"

  def fetch(
    loggedInAsPsa: Boolean
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    http
      .get(url"$url")
      .setHeader("loggedInAsPsa" -> String.valueOf(loggedInAsPsa))
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[MinimalDetails]
      .map(Right(_))
      .recover {
        case e @ WithStatusCode(NOT_FOUND) if e.message.contains(Constants.detailsNotFound) =>
          Left(DetailsNotFound)
        case e @ WithStatusCode(FORBIDDEN) if e.message.contains(Constants.delimitedPSA) =>
          Left(DelimitedAdmin)
      }
      .tapError(_ =>
        Future.successful(logger.warn(s"Failed to fetch minimal details for loggedInAsPsa and $loggedInAsPsa"))
      )
}

sealed trait MinimalDetailsError

object MinimalDetailsError {

  case object DelimitedAdmin extends MinimalDetailsError
  case object DetailsNotFound extends MinimalDetailsError
}

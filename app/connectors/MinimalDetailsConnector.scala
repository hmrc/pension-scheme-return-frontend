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
import com.google.inject.ImplementedBy
import config.{Constants, FrontendAppConfig}
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import models.PensionSchemeId.{PsaId, PspId}
import connectors.MinimalDetailsError.{DelimitedAdmin, DetailsNotFound}
import play.api.Logger
import models.MinimalDetails
import uk.gov.hmrc.http.client.HttpClientV2
import utils.FutureUtils.FutureOps
import play.api.http.Status.{FORBIDDEN, NOT_FOUND}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class MinimalDetailsConnectorImpl @Inject()(appConfig: FrontendAppConfig, http: HttpClientV2)
    extends MinimalDetailsConnector {

  private val url = s"${appConfig.pensionsAdministrator}/pension-administrator/get-minimal-psa"

  override def fetch(
    psaId: PsaId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    fetch("psaId", psaId.value)

  override def fetch(
    pspId: PspId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    fetch("pspId", pspId.value)

  // API 1442 (Get psa/psp minimal details)
  private def fetch(
    idType: String,
    idValue: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    http
      .get(url"$url")
      .setHeader(idType -> idValue)
      .execute[MinimalDetails]
      .map(Right(_))
      .recover {
        case e @ WithStatusCode(NOT_FOUND) if e.message.contains(Constants.detailsNotFound) =>
          Left(DetailsNotFound)
        case e @ WithStatusCode(FORBIDDEN) if e.message.contains(Constants.delimitedPSA) =>
          Left(DelimitedAdmin)
      }
      .tapError(t => Future.successful(logger.error(s"Failed to fetch minimal details with message ${t.getMessage}")))
}

@ImplementedBy(classOf[MinimalDetailsConnectorImpl])
trait MinimalDetailsConnector {

  protected val logger: Logger = Logger(classOf[MinimalDetailsConnector])

  def fetch(
    psaId: PsaId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]]

  def fetch(
    pspId: PspId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]]
}

sealed trait MinimalDetailsError

object MinimalDetailsError {

  case object DelimitedAdmin extends MinimalDetailsError
  case object DetailsNotFound extends MinimalDetailsError
}

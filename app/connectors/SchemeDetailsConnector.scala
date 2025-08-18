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

import uk.gov.hmrc.http.HttpReads.Implicits.{readFromJson, readOptionOfNotFound}
import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import models.SchemeId.Srn
import models.PensionSchemeId.{PsaId, PspId}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import models.{SchemeDetails, SchemeId}
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class SchemeDetailsConnectorImpl @Inject() (appConfig: FrontendAppConfig, http: HttpClientV2)
    extends SchemeDetailsConnector {

  private def url(relativePath: String) = s"${appConfig.pensionsScheme}$relativePath"

  // API 1444 (Get scheme details)
  override def details(
    psaId: PsaId,
    schemeId: SchemeId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]] =
    http
      .get(url"${url(s"/pensions-scheme/scheme/${schemeId.value}")}")
      .transform(
        _.addHttpHeaders(
          "idNumber" -> schemeId.value,
          "schemeIdType" -> schemeId.idType,
          "psaId" -> psaId.value
        )
      )
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[Option[SchemeDetails]]
      .recoverWith { t =>
        logger.warn(s"Failed to fetch scheme details $schemeId for psa with message ${t.getMessage}", t)
        Future.failed(t) // preserve the original failure
      }

  // API 1444 (Get scheme details)
  override def details(
    pspId: PspId,
    schemeId: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]] =
    http
      .get(url"${url(s"/pensions-scheme/psp-scheme/${schemeId.value}")}")
      .transform(
        _.addHttpHeaders(
          "pspId" -> pspId.value,
          "srn" -> schemeId.value
        )
      )
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[Option[SchemeDetails]]
      .recoverWith { t =>
        logger.warn(s"Failed to fetch scheme details $schemeId for psp with message ${t.getMessage}", t)
        Future.failed(t)
      }

}

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  protected val logger: Logger = Logger(classOf[SchemeDetailsConnector])

  def details(psaId: PsaId, schemeId: SchemeId)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[SchemeDetails]]

  def details(pspId: PspId, schemeId: Srn)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[SchemeDetails]]

}

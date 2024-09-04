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
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import models.PensionSchemeId.{PsaId, PspId}
import play.api.Logger
import models.{ListMinimalSchemeDetails, SchemeDetails, SchemeId}
import uk.gov.hmrc.http.client.HttpClientV2
import utils.FutureUtils.FutureOps
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class SchemeDetailsConnectorImpl @Inject()(appConfig: FrontendAppConfig, http: HttpClientV2)
    extends SchemeDetailsConnector {

  private def url(relativePath: String) = s"${appConfig.pensionsScheme}$relativePath"

  // API 1444 (Get scheme details)
  override def details(
    psaId: PsaId,
    schemeId: SchemeId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]] =
    http
      .get(url"${url("/pensions-scheme/scheme")}")
      .transform(
        _.addHttpHeaders(
          "idNumber" -> schemeId.value,
          "schemeIdType" -> schemeId.idType,
          "psaId" -> psaId.value
        )
      )
      .execute[Option[SchemeDetails]]
      .tapError { t =>
        Future.successful(
          logger.warn(s"Failed to fetch scheme details $schemeId for psa with message ${t.getMessage}")
        )
      }

  // API 1444 (Get scheme details)
  override def details(
    pspId: PspId,
    schemeId: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]] =
    http
      .get(url"${url("/pensions-scheme/psp-scheme")}")
      .transform(
        _.addHttpHeaders(
          "pspId" -> pspId.value,
          "srn" -> schemeId.value
        )
      )
      .execute[Option[SchemeDetails]]
      .tapError { t =>
        Future.successful(
          logger.warn(s"Failed to fetch scheme details $schemeId for psp with message ${t.getMessage}")
        )
      }

  override def checkAssociation(
    psaId: PsaId,
    schemeId: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    checkAssociation(psaId.value, "psaId", schemeId)

  override def checkAssociation(
    pspId: PspId,
    schemeId: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    checkAssociation(pspId.value, "pspId", schemeId)

  private def checkAssociation(idValue: String, idType: String, srn: Srn)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    http
      .get(url"${url("/pensions-scheme/is-psa-associated")}")
      .transform(
        _.addHttpHeaders(
          idType -> idValue,
          "schemeReferenceNumber" -> srn.value,
          "Content-Type" -> "application/json"
        )
      )
      .execute[Boolean]
      .tapError { t =>
        Future.successful(
          logger.error(s"Failed check association for scheme $srn for $idType with message ${t.getMessage}")
        )
      }

  def listSchemeDetails(
    psaId: PsaId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]] =
    listSchemeDetails(psaId.value, "psaid")

  def listSchemeDetails(
    pspId: PspId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]] =
    listSchemeDetails(pspId.value, "pspid")

  private def listSchemeDetails(
    idValue: String,
    idType: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]] =
    http
      .get(url"${url("/pensions-scheme/list-of-schemes")}")
      .transform(
        _.addHttpHeaders(
          "idValue" -> idValue,
          "idType" -> idType
        )
      )
      .execute[ListMinimalSchemeDetails]
      .map(Some(_))
      .recover {
        case WithStatusCode(NOT_FOUND) => None
      }
      .tapError { t =>
        Future.successful(logger.error(s"Failed list scheme details for $idType with message ${t.getMessage}"))
      }
}

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  protected val logger: Logger = Logger(classOf[SchemeDetailsConnector])

  def details(psaId: PsaId, schemeId: SchemeId)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[SchemeDetails]]

  def details(pspId: PspId, schemeId: Srn)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[SchemeDetails]]

  def checkAssociation(psaId: PsaId, schemeId: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]

  def checkAssociation(pspId: PspId, schemeId: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]

  def listSchemeDetails(
    psaId: PsaId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]]

  def listSchemeDetails(
    pspId: PspId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]]
}

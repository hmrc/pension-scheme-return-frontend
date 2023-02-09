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

import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import models.PensionSchemeId.{PsaId, PspId}
import models.SchemeId.Srn
import models.{ListMinimalSchemeDetails, SchemeDetails, SchemeId}
import play.api.Logger
import uk.gov.hmrc.http.HttpReads.Implicits.{readFromJson, readOptionOfNotFound}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.FutureUtils.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsConnectorImpl @Inject()(appConfig: FrontendAppConfig, http: HttpClient) extends SchemeDetailsConnector {

  private def url(relativePath: String) = s"${appConfig.pensionsScheme}$relativePath"

  override def details(psaId: PsaId, schemeId: SchemeId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]] = {

    val headers = List(
      "idNumber"     -> schemeId.value,
      "schemeIdType" -> schemeId.idType,
      "psaId"        -> psaId.value
    )

    http
      .GET[Option[SchemeDetails]](url("/pensions-scheme/scheme"), headers = headers)
      .tapError { t =>
        Future.successful(logger.error(s"Failed to fetch scheme details $schemeId for psa $psaId with message ${t.getMessage}"))
      }
  }

  override def details(pspId: PspId, schemeId: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]] = {

    val headers = List(
      "pspId" -> pspId.value,
      "srn"   -> schemeId.value
    )

    http
      .GET[Option[SchemeDetails]](url("/pensions-scheme/psp-scheme"), headers = headers)
      .tapError { t =>
        Future.successful(logger.error(s"Failed to fetch scheme details $schemeId for psp $pspId with message ${t.getMessage}"))
      }
  }

  override def checkAssociation(psaId: PsaId, schemeId: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    checkAssociation(psaId.value, "psaId", schemeId)

  override def checkAssociation(pspId: PspId, schemeId: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    checkAssociation(pspId.value, "pspId", schemeId)

  private def checkAssociation(idValue: String, idType: String, srn: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val headers = List(
      idType                  -> idValue,
      "schemeReferenceNumber" -> srn.value,
      "Content-Type"          -> "application/json"
    )

    http
      .GET[Boolean](url("/pensions-scheme/is-psa-associated"), headers = headers)
      .tapError { t =>
        Future.successful(logger.error(s"Failed check association for scheme $srn for $idType $idValue with message ${t.getMessage}"))
      }
  }

  def listSchemeDetails(psaId: PsaId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListMinimalSchemeDetails] =
    listSchemeDetails(psaId.value, "psaId")

  def listSchemeDetails(pspId: PspId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListMinimalSchemeDetails] =
    listSchemeDetails(pspId.value, "pspId")

  private def listSchemeDetails(idValue: String, idType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListMinimalSchemeDetails] = {

    val headers = List(
      "idValue" -> idValue,
      "idType"  -> idType
    )

    http
      .GET[ListMinimalSchemeDetails](url("/pensions-scheme/list-of-schemes"), headers = headers)
      .tapError { t =>
        Future.successful(logger.error(s"Failed list scheme details for $idType $idValue with message ${t.getMessage}"))
      }
  }
}

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  protected val logger: Logger = Logger(classOf[SchemeDetailsConnector])

  def details(psaId: PsaId, schemeId: SchemeId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]]
  def details(pspId: PspId, schemeId: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]]

  def checkAssociation(psaId: PsaId, schemeId: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]
  def checkAssociation(pspId: PspId, schemeId: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]

  def listSchemeDetails(psaId: PsaId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListMinimalSchemeDetails]
  def listSchemeDetails(pspId: PspId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListMinimalSchemeDetails]
}
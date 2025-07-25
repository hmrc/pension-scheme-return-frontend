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

package services

import connectors.PSRConnector
import models.SchemeId.Srn
import models.requests.{AllowedAccessRequest, DataRequest}
import models.backend.responses.{PsrVersionsForYearsResponse, PsrVersionsResponse}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class PsrVersionsService @Inject() (psrConnector: PSRConnector) {
  def getVersionsForYears(pstr: String, startDates: Seq[String], srn: Srn)(implicit
    request: AllowedAccessRequest[?],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[PsrVersionsForYearsResponse]] =
    psrConnector.getVersionsForYears(pstr, startDates, srn, controllers.routes.OverviewController.onPageLoad(srn))

  def getVersions(pstr: String, startDate: String, srn: Srn)(implicit
    request: DataRequest[?],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[PsrVersionsResponse]] =
    psrConnector.getVersions(pstr, startDate, srn, controllers.routes.OverviewController.onPageLoad(srn))
}

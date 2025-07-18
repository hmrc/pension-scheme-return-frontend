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
import models.requests.AllowedAccessRequest
import models.backend.responses.OverviewResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class PsrOverviewService @Inject() (psrConnector: PSRConnector) {
  def getOverview(pstr: String, fromDate: String, toDate: String, srn: Srn)(implicit
    request: AllowedAccessRequest[?],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[Seq[OverviewResponse]]] =
    psrConnector.getOverview(pstr, fromDate, toDate, srn, controllers.routes.OverviewController.onPageLoad(srn))
}

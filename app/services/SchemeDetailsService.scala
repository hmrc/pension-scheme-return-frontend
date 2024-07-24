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

import com.google.inject.ImplementedBy
import connectors.SchemeDetailsConnector
import models.SchemeId.Srn
import uk.gov.hmrc.http.HeaderCarrier
import models.{MinimalSchemeDetails, PensionSchemeId}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class SchemeDetailsServiceImpl @Inject()(schemeDetailsConnector: SchemeDetailsConnector) extends SchemeDetailsService {

  def getMinimalSchemeDetails(
    id: PensionSchemeId,
    srn: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[MinimalSchemeDetails]] =
    id.fold(
        schemeDetailsConnector.listSchemeDetails(_),
        schemeDetailsConnector.listSchemeDetails(_)
      )
      .map { allDetails =>
        allDetails.flatMap(_.schemeDetails.find(_.srn == srn.value))
      }
}

@ImplementedBy(classOf[SchemeDetailsServiceImpl])
trait SchemeDetailsService {

  def getMinimalSchemeDetails(id: PensionSchemeId, srn: Srn)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[MinimalSchemeDetails]]
}

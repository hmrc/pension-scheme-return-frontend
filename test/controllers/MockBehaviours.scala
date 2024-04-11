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

package controllers

import services.{PsrSubmissionService, SchemeDateService}
import org.mockito.Mockito.when
import config.Refined.Max3
import cats.data.NonEmptyList
import org.mockito.stubbing.OngoingStubbing
import models.DateRange
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

trait MockBehaviours {

  object MockSchemeDateService {
    def taxYearOrAccountingPeriods(
      returns: Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]
    )(
      implicit mock: SchemeDateService
    ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
      when(mock.taxYearOrAccountingPeriods(any())(any())).thenReturn(returns)

    def returnPeriods(
      returns: Option[NonEmptyList[DateRange]]
    )(implicit mock: SchemeDateService): OngoingStubbing[Option[NonEmptyList[DateRange]]] =
      when(mock.returnPeriods(any())(any())).thenReturn(returns)
  }

  object MockPSRSubmissionService {

    def submitPsrDetails()(implicit mock: PsrSubmissionService): OngoingStubbing[Future[Option[Unit]]] =
      when(mock.submitPsrDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(())))
  }
}

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

package controllers

import cats.data.NonEmptyList
import config.Refined.Max3
import models.DateRange
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.mockito.stubbing.ScalaOngoingStubbing
import services.{PSRSubmissionService, SchemeDateService}

import scala.concurrent.Future

trait MockBehaviours {

  object MockSchemeDateService {
    def taxYearOrAccountingPeriods(
      returns: Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]
    )(
      implicit mock: SchemeDateService
    ): ScalaOngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
      when(mock.taxYearOrAccountingPeriods(any())(any())).thenReturn(returns)

    def returnPeriods(
      returns: Option[NonEmptyList[DateRange]]
    )(implicit mock: SchemeDateService): ScalaOngoingStubbing[Option[NonEmptyList[DateRange]]] =
      when(mock.returnPeriods(any())(any())).thenReturn(returns)
  }

  object MockPSRSubmissionService {
    def submitMinimalRequiredDetails()(implicit mock: PSRSubmissionService): ScalaOngoingStubbing[Future[Unit]] =
      when(mock.submitMinimalRequiredDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(())))
  }
}

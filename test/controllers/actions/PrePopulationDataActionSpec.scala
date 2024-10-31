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

package controllers.actions

import play.api.test.FakeRequest
import services.PsrRetrievalService
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.mockito.Mockito._
import play.api.mvc.AnyContentAsEmpty
import controllers.ControllerBaseSpec
import repositories.SessionRepository
import models.requests.DataRequest
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any

import scala.concurrent.{ExecutionContext, Future}

class PrePopulationDataActionSpec extends ControllerBaseSpec with ScalaCheckPropertyChecks {

  val prePopulationDataAction =
    new PrePopulationDataActionProviderImpl(
      mockSessionRepository,
      mockPsrRetrievalService
    )(ExecutionContext.global)

  lazy val mockSessionRepository: SessionRepository = mock[SessionRepository]
  lazy val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]

  class Harness[A](request: DataRequest[A]) {

    def callTransform(optLastSubmittedPsrFbInPreviousYears: Option[String]): Future[DataRequest[A]] =
      prePopulationDataAction(optLastSubmittedPsrFbInPreviousYears).transform(request)
  }

  def harness[A](request: DataRequest[A]) = new Harness(request)

  override def beforeEach(): Unit = {
    reset(mockSessionRepository)
    reset(mockPsrRetrievalService)
  }

  val defaultDataRequest: DataRequest[AnyContentAsEmpty.type] =
    DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, emptyUserAnswers)

  "PrePopulationDataAction" - {

    "should not perform transform when optLastSubmittedPsrFbInPreviousYears None" in {
      val action = harness(defaultDataRequest)

      val result = action.callTransform(None).futureValue

      result mustBe defaultDataRequest
      verify(mockSessionRepository, never).set(any())
      verify(mockPsrRetrievalService, never).getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(
        any(),
        any(),
        any()
      )
    }

    "should fetch base return save on cache and return when optLastSubmittedPsrFbInPreviousYears exist" in {
      when(
        mockPsrRetrievalService
          .getAndTransformStandardPsrDetails(optFbNumber = ArgumentMatchers.eq(Some("1")), any(), any(), any(), any())(
            any(),
            any(),
            any()
          )
      ).thenReturn(Future.successful(defaultUserAnswers))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(()))
      val action = harness(defaultDataRequest)

      val result = action.callTransform(Some("1")).futureValue

      result.userAnswers mustBe defaultUserAnswers
      verify(mockSessionRepository, times(1)).set(any())
      verify(mockPsrRetrievalService, times(1)).getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(
        any(),
        any(),
        any()
      )
    }

  }

}

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
import play.api.mvc.{ActionTransformer, AnyContentAsEmpty}
import controllers.ControllerBaseSpec
import models.SchemeId.Srn
import pages.nonsipp.{FbVersionPage, WhichTaxYearPage}
import repositories.SessionRepository
import models.UserAnswers
import models.requests.{AllowedAccessRequest, OptionalDataRequest}
import org.mockito.ArgumentMatchers.any
import utils.{BaseSpec, CommonTestValues}
import org.mockito.Mockito.when
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DataRetrievalETMPActionSpec extends ControllerBaseSpec with CommonTestValues {
  private val srn: Srn = srnGen.sample.value
  private val version1 = 1
  private val version2 = 2
  private val request: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
  ).sample.value
  private val userAnswers = UserAnswers(request.getUserId + request.srn)
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    .unsafeSet(FbVersionPage(srn), version)
    .unsafeSet(WhichTaxYearPage(srn), dateRange)

  "getAndTransformVersionForYear" - {
    "should returns user answers when getAndTransformStandardPsrDetails returns empty user answers" in {

      val sessionRepository = mock[SessionRepository]
      val psrRetrievalService = mock[PsrRetrievalService]
      when(sessionRepository.get(any())).thenReturn(Future(None))
      when(
        psrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(userAnswers))

      val action = new DataRetrievalETMPAction(sessionRepository, psrRetrievalService)

      val result = action.getAndTransformVersionForYear(startDate, version1, request).futureValue

      result.userAnswers mustBe defined
      result.previousUserAnswers must not be defined
    }
  }

  "getAndTransformCurrentAndPreviousVersionForYear" - {
    "should returns user answers and previous user answers" in {

      val sessionRepository = mock[SessionRepository]
      val psrRetrievalService = mock[PsrRetrievalService]
      when(sessionRepository.get(any())).thenReturn(Future(None))
      when(
        psrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      )
        .thenReturn(Future(userAnswers))

      val action = new DataRetrievalETMPAction(sessionRepository, psrRetrievalService)

      val result =
        action.getAndTransformCurrentAndPreviousVersionForYear(startDate, version2, version1, request).futureValue

      result.userAnswers mustBe defined
      result.previousUserAnswers mustBe defined
    }
    "should returns user answers and no previous user answers if previous is empty" in {

      val sessionRepository = mock[SessionRepository]
      val psrRetrievalService = mock[PsrRetrievalService]
      when(sessionRepository.get(any())).thenReturn(Future(None))
      when(
        psrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      )
        .thenReturn(Future(emptyUserAnswers))

      val action = new DataRetrievalETMPAction(sessionRepository, psrRetrievalService)

      val result =
        action.getAndTransformCurrentAndPreviousVersionForYear(startDate, version2, version1, request).futureValue

      result.userAnswers mustBe defined
      result.previousUserAnswers must not be defined
    }
  }

  "getAndTransformFbNumber" - {
    "should returns user answers by fbNumber if there is a previous version" in {

      val sessionRepository = mock[SessionRepository]
      val psrRetrievalService = mock[PsrRetrievalService]
      when(sessionRepository.get(any())).thenReturn(Future(None))
      when(
        psrRetrievalService.getStandardPsrDetails(any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(Some(minimalSubmissionWithVersion("002"))))
      when(
        psrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(userAnswers))
      when(psrRetrievalService.transformPsrDetails(any(), any())(using any())).thenReturn(Future(userAnswers))

      val action = new DataRetrievalETMPAction(sessionRepository, psrRetrievalService)
      val result = action.getAndTransformFbNumber(fbNumber, request).futureValue

      result.userAnswers mustBe defined
      result.previousUserAnswers mustBe defined
    }

    "should returns user answers by fbNumber if there are no psrDetails" in {

      val sessionRepository = mock[SessionRepository]
      val psrRetrievalService = mock[PsrRetrievalService]
      when(sessionRepository.get(any())).thenReturn(Future(None))
      when(
        psrRetrievalService.getStandardPsrDetails(any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(None))
      when(
        psrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(userAnswers))
      when(psrRetrievalService.transformPsrDetails(any(), any())(using any())).thenReturn(Future(userAnswers))

      val action = new DataRetrievalETMPAction(sessionRepository, psrRetrievalService)
      val result = action.getAndTransformFbNumber(fbNumber, request).futureValue

      result.userAnswers mustBe defined
      result.previousUserAnswers must not be defined
    }
  }

  "implementation" - {
    "versionForYear should return the right action transformer" in {

      val sessionRepository = mock[SessionRepository]
      val psrRetrievalService = mock[PsrRetrievalService]
      when(sessionRepository.get(any())).thenReturn(Future(None))
      when(
        psrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(userAnswers))

      val actionProvider = new DataRetrievalETMPActionProviderImpl(sessionRepository, psrRetrievalService)
      val result = actionProvider.versionForYear(startDate, version1)

      result shouldBe a[ActionTransformer[AllowedAccessRequest, OptionalDataRequest]]
    }

    "currentAndPreviousVersionForYear should return the right action transformer" in {

      val sessionRepository = mock[SessionRepository]
      val psrRetrievalService = mock[PsrRetrievalService]
      when(sessionRepository.get(any())).thenReturn(Future(None))
      when(
        psrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(userAnswers))

      val actionProvider = new DataRetrievalETMPActionProviderImpl(sessionRepository, psrRetrievalService)
      val result = actionProvider.currentAndPreviousVersionForYear(startDate, version2, version1)
      result shouldBe a[ActionTransformer[AllowedAccessRequest, OptionalDataRequest]]
    }

    "fbNumber should return the right action transformer" in {
      val sessionRepository = mock[SessionRepository]
      val psrRetrievalService = mock[PsrRetrievalService]
      when(sessionRepository.get(any())).thenReturn(Future(None))
      when(
        psrRetrievalService.getStandardPsrDetails(any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(Some(minimalSubmissionWithVersion("002"))))
      when(
        psrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using
          any(),
          any(),
          any()
        )
      ).thenReturn(Future(userAnswers))
      when(psrRetrievalService.transformPsrDetails(any(), any())(using any())).thenReturn(Future(userAnswers))

      val actionProvider = new DataRetrievalETMPActionProviderImpl(sessionRepository, psrRetrievalService)
      val result = actionProvider.fbNumber(fbNumber)
      result shouldBe a[ActionTransformer[AllowedAccessRequest, OptionalDataRequest]]
    }
  }

  private def minimalSubmissionWithVersion(version: String) =
    minimalSubmissionData.copy(minimalRequiredSubmission =
      minimalSubmissionData.minimalRequiredSubmission.copy(
        reportDetails = minimalSubmissionData.minimalRequiredSubmission.reportDetails.copy(fbVersion = Some(version))
      )
    )
}

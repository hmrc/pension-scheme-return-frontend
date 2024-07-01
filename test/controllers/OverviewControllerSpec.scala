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

import play.api.test.FakeRequest
import services.{PsrOverviewService, PsrRetrievalService, PsrVersionsService}
import play.api.inject.bind
import views.html.OverviewView
import models.backend.responses.PsrReportType
import viewmodels.OverviewSummary
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule

import scala.concurrent.Future

class OverviewControllerSpec extends ControllerBaseSpec with CommonTestValues {

  lazy val onPageLoad: String = routes.OverviewController.onPageLoad(srn).url
  lazy val onSelectStart: String =
    routes.OverviewController.onSelectStart(srn, commonStartDate, commonVersion, PsrReportType.Standard.name).url
  lazy val onSelectContinue: String =
    routes.OverviewController.onSelectContinue(srn, commonStartDate, commonVersion, PsrReportType.Standard.name).url
  lazy val onSelectViewAndChange: String =
    routes.OverviewController.onSelectViewAndChange(srn, commonFbNumber, PsrReportType.Standard.name).url

  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]
  private implicit val mockPsrOverviewService: PsrOverviewService = mock[PsrOverviewService]
  private implicit val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService),
    bind[PsrOverviewService].toInstance(mockPsrOverviewService),
    bind[PsrVersionsService].toInstance(mockPsrVersionsService)
  )

  override protected def beforeAll(): Unit = {
    reset(mockPsrRetrievalService)
    reset(mockPsrOverviewService)
    reset(mockPsrVersionsService)
  }

  "OverviewController" - {

    "onPageLoads returns OK and the correct view - when empty responses returned" in runningApplication {
      implicit app =>
        when(mockPsrOverviewService.getOverview(any(), any(), any())(any(), any())).thenReturn(
          Future.successful(Some(Seq()))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any())(any(), any())).thenReturn(
          Future.successful(Seq())
        )

        val view = injected[OverviewView]
        val request = FakeRequest(GET, onPageLoad)
        val seqOfOutstandingReturns: Seq[OverviewSummary] = Seq()
        val seqOfPreviousReturns: Seq[OverviewSummary] = Seq()

        val result = route(app, request).value
        val expectedView = view(seqOfOutstandingReturns, seqOfPreviousReturns, defaultSchemeDetails.schemeName)(
          request,
          createMessages(app)
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView.toString
    }

    "onPageLoads returns OK and expected content - when submitted responses returned" in runningApplication {
      implicit app =>
        when(mockPsrOverviewService.getOverview(any(), any(), any())(any(), any())).thenReturn(
          Future.successful(Some(overviewResponse))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any())(any(), any())).thenReturn(
          Future.successful(versionsForYearsResponse)
        )
        val request = FakeRequest(GET, onPageLoad)

        val result = route(app, request).value

        status(result) mustEqual OK
        val content = contentAsString(result)
        content must include("<td class=\"govuk-table__cell\">first last</td>")
        (content must not).include("<td class=\"govuk-table__cell\">Changes not submitted</td>")
        content must include("<td class=\"govuk-table__cell\">Not started</td>")
        content must include("<caption class=\"govuk-table__caption govuk-table__caption--m\">Submitted</caption>")
        (content must not).include(
          "<caption class=\"govuk-table__caption govuk-table__caption--m\">Submitted with changes in progress</caption>"
        )
    }

    "onPageLoads returns OK and expected content - when in progress responses returned" in runningApplication {
      implicit app =>
        when(mockPsrOverviewService.getOverview(any(), any(), any())(any(), any())).thenReturn(
          Future.successful(Some(overviewResponse))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any())(any(), any())).thenReturn(
          Future.successful(versionsForYearsInProgressResponse)
        )
        val request = FakeRequest(GET, onPageLoad)

        val result = route(app, request).value

        status(result) mustEqual OK
        val content = contentAsString(result)
        (content must not).include("<td class=\"govuk-table__cell\">first last</td>")
        content must include("<td class=\"govuk-table__cell\">Changes not submitted</td>")
        content must include("<td class=\"govuk-table__cell\">Not started</td>")
        (content must not).include(
          "<caption class=\"govuk-table__caption govuk-table__caption--m\">Submitted</caption>"
        )
        content must include(
          "<caption class=\"govuk-table__caption govuk-table__caption--m\">Submitted with changes in progress</caption>"
        )
    }

    "onSelectStart redirects to what you will need page" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onSelectStart)

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.WhatYouWillNeedController
        .onPageLoad(srn, "", commonStartDate, commonVersion)
        .url
    }

    "onSelectContinue redirects to what you will need page" in runningApplication { implicit app =>
      when(
        mockPsrRetrievalService
          .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(defaultUserAnswers)
      )

      val request = FakeRequest(GET, onSelectContinue)

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.nonsipp.routes.TaskListController.onPageLoad(srn).url
    }

    "onSelectViewAndChange redirects to the task list page" in runningApplication { implicit app =>
      when(
        mockPsrRetrievalService
          .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(defaultUserAnswers)
      )

      val request = FakeRequest(GET, onSelectViewAndChange)

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.nonsipp.routes.TaskListController.onPageLoad(srn).url
    }

  }
}

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
import services._
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import play.api.inject.bind
import views.html.OverviewView
import eu.timepit.refined.refineMV
import pages.nonsipp.WhichTaxYearPage
import models.backend.responses.PsrReportType
import models.CheckMode
import uk.gov.hmrc.time.TaxYear
import org.scalatest.Inspectors.forAll
import viewmodels.OverviewSummary
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import pages.nonsipp.memberdetails.DoesMemberHaveNinoPage
import org.mockito.Mockito._
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule

import scala.concurrent.Future

import java.time.LocalDate

class OverviewControllerSpec extends ControllerBaseSpec with CommonTestValues {

  lazy val onPageLoad: String = routes.OverviewController.onPageLoad(srn).url
  def onSelectStart(lastSubmittedPsrFbInPreviousYears: Option[String]): String =
    routes.OverviewController
      .onSelectStart(
        srn,
        commonStartDate,
        commonVersion,
        PsrReportType.Standard.name,
        lastSubmittedPsrFbInPreviousYears
      )
      .url
  lazy val onSelectContinue: String =
    routes.OverviewController
      .onSelectContinue(srn, commonStartDate, commonVersion, Some(commonFbNumber), PsrReportType.Standard.name, None)
      .url
  lazy val onSelectViewAndChange: String =
    routes.OverviewController
      .onSelectViewAndChange(srn, commonFbNumber, commonStartDate, PsrReportType.Standard.name)
      .url

  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]
  private implicit val mockPsrOverviewService: PsrOverviewService = mock[PsrOverviewService]
  private implicit val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]
  private implicit val mockPrePopulationService: PrePopulationService = mock[PrePopulationService]
  private implicit val mockTaxYearService: TaxYearService = mock[TaxYearService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService),
    bind[PsrOverviewService].toInstance(mockPsrOverviewService),
    bind[PsrVersionsService].toInstance(mockPsrVersionsService),
    bind[PrePopulationService].toInstance(mockPrePopulationService),
    bind[TaxYearService].toInstance(mockTaxYearService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrRetrievalService)
    reset(mockPsrOverviewService)
    reset(mockPsrVersionsService)
    reset(mockPrePopulationService)
    reset(mockTaxYearService)
  }

  "OverviewController" - {

    val dates = List(
      (2021, 2021, 2022),
      (2022, 2021, 2023),
      (2026, 2021, 2027),
      (2027, 2021, 2028),
      (2030, 2024, 2031)
    )

    forAll(dates) {
      case (currentPeriodStartYear: Int, earliestStartYear: Int, latestStartYear: Int) =>
        s"onPageLoads uses passes correct date range to the overview service when the current year is $currentPeriodStartYear" in runningApplication {
          implicit app =>
            when(mockTaxYearService.current).thenReturn(TaxYear(currentPeriodStartYear))
            when(mockPsrOverviewService.getOverview(any(), any(), any(), any())(any(), any(), any())).thenReturn(
              Future.successful(None)
            )
            when(mockPsrVersionsService.getVersionsForYears(any(), any(), any())(any(), any(), any())).thenReturn(
              Future.successful(Seq())
            )

            val request = FakeRequest(GET, onPageLoad)

            val allYears = Seq.range(latestStartYear - 1, earliestStartYear - 1, -1).map(x => s"$x-04-06")

            whenReady(route(app, request).value) { result =>
              verify(mockPsrOverviewService, times(1)).getOverview(
                any(),
                eqTo(s"$earliestStartYear-04-06"),
                eqTo(s"$latestStartYear-04-06"),
                any()
              )(any(), any(), any())

              verify(mockPsrVersionsService, times(1)).getVersionsForYears(
                any(),
                eqTo(allYears),
                any()
              )(any(), any(), any())

              result
            }
        }
    }

    "onPageLoads returns OK and the correct view - when empty responses returned" in runningApplication {
      implicit app =>
        when(mockTaxYearService.current).thenReturn(TaxYear(LocalDate.now().getYear))
        when(mockPsrOverviewService.getOverview(any(), any(), any(), any())(any(), any(), any())).thenReturn(
          Future.successful(Some(Seq()))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any(), any())(any(), any(), any())).thenReturn(
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
        verify(mockPsrOverviewService, times(1)).getOverview(any(), any(), any(), any())(any(), any(), any())
        verify(mockPsrVersionsService, times(1)).getVersionsForYears(any(), any(), any())(any(), any(), any())
        verify(mockPrePopulationService, never).findLastSubmittedPsrFbInPreviousYears(any(), any())
    }

    "onPageLoads returns OK and expected content - when submitted responses returned" in runningApplication {
      implicit app =>
        when(mockTaxYearService.current).thenReturn(TaxYear(LocalDate.now().getYear))
        when(mockPsrOverviewService.getOverview(any(), any(), any(), any())(any(), any(), any())).thenReturn(
          Future.successful(Some(overviewResponse))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any(), any())(any(), any(), any())).thenReturn(
          Future.successful(versionsForYearsResponse)
        )
        when(mockPrePopulationService.findLastSubmittedPsrFbInPreviousYears(any(), any())).thenReturn(
          None
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
        verify(mockPsrOverviewService, times(1)).getOverview(any(), any(), any(), any())(any(), any(), any())
        verify(mockPsrVersionsService, times(1)).getVersionsForYears(any(), any(), any())(any(), any(), any())
        verify(mockPrePopulationService, times(1)).findLastSubmittedPsrFbInPreviousYears(any(), any())
    }

    "onPageLoads returns OK and expected content - when in progress responses returned" in runningApplication {
      implicit app =>
        when(mockTaxYearService.current).thenReturn(TaxYear(LocalDate.now().getYear))
        when(mockPsrOverviewService.getOverview(any(), any(), any(), any())(any(), any(), any())).thenReturn(
          Future.successful(Some(overviewResponse))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any(), any())(any(), any(), any())).thenReturn(
          Future.successful(versionsForYearsInProgressResponse)
        )
        when(mockPrePopulationService.findLastSubmittedPsrFbInPreviousYears(any(), any())).thenReturn(
          Some("1")
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
        verify(mockPsrOverviewService, times(1)).getOverview(any(), any(), any(), any())(any(), any(), any())
        verify(mockPsrVersionsService, times(1)).getVersionsForYears(any(), any(), any())(any(), any(), any())
        verify(mockPrePopulationService, times(1)).findLastSubmittedPsrFbInPreviousYears(any(), any())
    }

    "onSelectStart redirects to what you will need page" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onSelectStart(None))

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.WhatYouWillNeedController
        .onPageLoad(srn, "", commonStartDate, commonVersion)
        .url
    }

    "onSelectStart redirects to check updated information page when pre-population" in runningApplication {
      implicit app =>
        val request = FakeRequest(GET, onSelectStart(Some("01")))

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckUpdateInformationController
          .onPageLoad(srn)
          .url
    }

    "onSelectContinue redirects to task list page" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onSelectContinue)

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.nonsipp.routes.TaskListController.onPageLoad(srn).url
    }

    "onSelectViewAndChange redirects to the task list page when members empty" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onSelectViewAndChange)

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.nonsipp.routes.TaskListController.onPageLoad(srn).url
    }

    "onSelectViewAndChange redirects to the task list page when members under threshold" in {
      val ua = emptyUserAnswers.unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersUnderThreshold)
      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, onSelectViewAndChange)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.nonsipp.routes.TaskListController.onPageLoad(srn).url
      }
    }

    "onSelectViewAndChange redirects to the task list page when members over threshold and previous userAnswers has some memberDetails" in {
      val currentUA = emptyUserAnswers.unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
      val preUA = currentUA.unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), true)

      running(_ => applicationBuilder(userAnswers = Some(currentUA), previousUserAnswers = Some(preUA))) { app =>
        val request = FakeRequest(GET, onSelectViewAndChange)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.nonsipp.routes.TaskListController.onPageLoad(srn).url
      }
    }

    "onSelectViewAndChange redirects to the journeyRecovery page when members over threshold and previous userAnswers empty and no TaxYear data" in {
      val currentUA = emptyUserAnswers.unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)

      running(_ => applicationBuilder(userAnswers = Some(currentUA))) { app =>
        val request = FakeRequest(GET, onSelectViewAndChange)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "onSelectViewAndChange redirects to the BasicDetailsCYA page when members over threshold and no previous userAnswers and no previous psr return at all" in {
      when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.successful(Seq())
      )
      val currentUA = emptyUserAnswers
        .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
        .unsafeSet(WhichTaxYearPage(srn), dateRange)

      running(_ => applicationBuilder(userAnswers = Some(currentUA))) { app =>
        val request = FakeRequest(GET, onSelectViewAndChange)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
          .onPageLoad(srn, CheckMode)
          .url
      }
    }

    "onSelectViewAndChange redirects to the BasicDetailsCYA page " +
      "when members over threshold and no previous userAnswers and no previous psr return with memberDetails" in {
      when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.successful(versionsResponse)
      )
      when(
        mockPsrRetrievalService
          .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Future.successful(emptyUserAnswers))
      val currentUA = emptyUserAnswers
        .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
        .unsafeSet(WhichTaxYearPage(srn), dateRange)

      running(_ => applicationBuilder(userAnswers = Some(currentUA))) { app =>
        val request = FakeRequest(GET, onSelectViewAndChange)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
          .onPageLoad(srn, CheckMode)
          .url
      }
    }

    "onSelectViewAndChange redirects to the task list page " +
      "when members over threshold and no previous userAnswers and previous psr return with memberDetails" in {
      val currentUA = emptyUserAnswers
        .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
        .unsafeSet(WhichTaxYearPage(srn), dateRange)
      when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.successful(versionsResponse)
      )
      when(
        mockPsrRetrievalService
          .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Future.successful(currentUA))
      running(_ => applicationBuilder(userAnswers = Some(currentUA))) { app =>
        val request = FakeRequest(GET, onSelectViewAndChange)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
          .onPageLoad(srn, CheckMode)
          .url
      }
    }
  }
}

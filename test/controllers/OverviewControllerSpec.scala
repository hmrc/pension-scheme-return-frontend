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
import utils.IntUtils.given
import pages.nonsipp.WhichTaxYearPage
import models.backend.responses._
import models.CheckMode
import viewmodels.OverviewSummary
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.memberdetails.DoesMemberHaveNinoPage
import org.mockito.Mockito._
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule

import scala.concurrent.Future

import java.time.{LocalDate, LocalDateTime}

class OverviewControllerSpec extends ControllerBaseSpec with ControllerBehaviours with CommonTestValues {

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
  private def onSelectContinue(version: String): String =
    routes.OverviewController
      .onSelectContinue(srn, commonStartDate, version, Some(commonFbNumber), PsrReportType.Standard.name, None)
      .url
  lazy val onSelectViewAndChange: String =
    routes.OverviewController
      .onSelectViewAndChange(srn, commonFbNumber, commonStartDate, PsrReportType.Standard.name)
      .url
  def onSelectStartSipp(lastSubmittedPsrFbInPreviousYears: Option[String]): String =
    routes.OverviewController
      .onSelectStart(
        srn,
        commonStartDate,
        commonVersion,
        PsrReportType.Sipp.name,
        lastSubmittedPsrFbInPreviousYears
      )
      .url
  private def onSelectContinueSipp(version: String): String =
    routes.OverviewController
      .onSelectContinue(srn, commonStartDate, version, Some(commonFbNumber), PsrReportType.Sipp.name, None)
      .url
  lazy val onSelectViewAndChangeSipp: String =
    routes.OverviewController
      .onSelectViewAndChange(srn, commonFbNumber, commonStartDate, PsrReportType.Sipp.name)
      .url
  val sippStartUrl = s"http://localhost:10703/pension-scheme-return-sipp/${srn.value}/what-you-will-need"
  val sippViewOrChangeUrl = s"http://localhost:10703/pension-scheme-return-sipp/${srn.value}/view-change-question"

  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]
  private implicit val mockPsrOverviewService: PsrOverviewService = mock[PsrOverviewService]
  private implicit val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]
  private implicit val mockPrePopulationService: PrePopulationService = mock[PrePopulationService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService),
    bind[PsrOverviewService].toInstance(mockPsrOverviewService),
    bind[PsrVersionsService].toInstance(mockPsrVersionsService),
    bind[PrePopulationService].toInstance(mockPrePopulationService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrRetrievalService)
    reset(mockPsrOverviewService)
    reset(mockPsrVersionsService)
    reset(mockPrePopulationService)
  }

  val compiledOverviewResponse: Seq[OverviewResponse] =
    Seq(
      OverviewResponse(
        periodStartDate = LocalDate.parse("2022-04-06"),
        periodEndDate = LocalDate.parse("2023-04-05"),
        numberOfVersions = Some(1),
        submittedVersionAvailable = Some(YesNo.No),
        compiledVersionAvailable = Some(YesNo.Yes),
        ntfDateOfIssue = Some(LocalDate.parse("2022-12-06")),
        psrDueDate = Some(LocalDate.parse("2023-03-31")),
        psrReportType = Some(PsrReportType.Standard),
        tpssReportPresent = None
      )
    )

  val compiledVersionsResponse2022: PsrVersionsResponse = PsrVersionsResponse(
    startDate = Some(LocalDate.parse("2022-04-06")),
    reportFormBundleNumber = commonFbNumber,
    reportVersion = commonVersion.toInt,
    reportStatus = ReportStatus.ReportStatusCompiled,
    compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
    reportSubmitterDetails = None,
    psaDetails = None
  )

  val compiledVersionsForYearsInProgressResponse: Seq[PsrVersionsForYearsResponse] =
    Seq(
      PsrVersionsForYearsResponse(
        startDate = "2022-04-06",
        data = Seq(compiledVersionsResponse2022)
      )
    )

  "OverviewController" - {

    "onPageLoads returns OK and the correct view - when empty responses returned" in runningApplication {
      implicit app =>
        when(mockPsrOverviewService.getOverview(any(), any(), any(), any())(using any(), any(), any())).thenReturn(
          Future.successful(Some(Seq()))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any(), any())(using any(), any(), any())).thenReturn(
          Future.successful(Seq())
        )

        val view = injected[OverviewView]
        val request = FakeRequest(GET, onPageLoad)
        val seqOfOutstandingReturns: Seq[OverviewSummary] = Seq()
        val seqOfPreviousReturns: Seq[OverviewSummary] = Seq()
        val dashboardUrl = s"http://localhost:8204/manage-pension-schemes/pension-scheme-summary/${srn.value}"

        val result = route(app, request).value
        val expectedView =
          view(seqOfOutstandingReturns, seqOfPreviousReturns, defaultSchemeDetails.schemeName, dashboardUrl)(using
            request,
            createMessages(using app)
          )

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView.toString
        verify(mockPsrOverviewService, times(1)).getOverview(any(), any(), any(), any())(using any(), any(), any())
        verify(mockPsrVersionsService, times(1)).getVersionsForYears(any(), any(), any())(using any(), any(), any())
        verify(mockPrePopulationService, never).findLastSubmittedPsrFbInPreviousYears(any(), any())
    }

    "onPageLoads returns OK and expected content - when submitted responses returned" in runningApplication {
      implicit app =>
        when(mockPsrOverviewService.getOverview(any(), any(), any(), any())(using any(), any(), any())).thenReturn(
          Future.successful(Some(overviewResponse))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any(), any())(using any(), any(), any())).thenReturn(
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
        content must include(
          "<caption class=\"govuk-table__caption govuk-table__caption--m\" id=\"table-caption-3\">Submitted</caption>"
        )
        (content must not).include(
          "<caption class=\"govuk-table__caption govuk-table__caption--m\" id=\"table-caption-2\">Submitted with changes in progress</caption>"
        )
        verify(mockPsrOverviewService, times(1)).getOverview(any(), any(), any(), any())(using any(), any(), any())
        verify(mockPsrVersionsService, times(1)).getVersionsForYears(any(), any(), any())(using any(), any(), any())
        verify(mockPrePopulationService, times(1)).findLastSubmittedPsrFbInPreviousYears(any(), any())
    }

    "onPageLoads returns OK and expected content - when in progress responses returned" in runningApplication {
      implicit app =>
        when(mockPsrOverviewService.getOverview(any(), any(), any(), any())(using any(), any(), any())).thenReturn(
          Future.successful(Some(overviewResponse))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any(), any())(using any(), any(), any())).thenReturn(
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
          "<caption class=\"govuk-table__caption govuk-table__caption--m\" id=\"table-caption-3\">Submitted</caption>"
        )
        content must include(
          "<caption class=\"govuk-table__caption govuk-table__caption--m\" id=\"table-caption-2\">Submitted with changes in progress</caption>"
        )
        verify(mockPsrOverviewService, times(1)).getOverview(any(), any(), any(), any())(using any(), any(), any())
        verify(mockPsrVersionsService, times(1)).getVersionsForYears(any(), any(), any())(using any(), any(), any())
        verify(mockPrePopulationService, times(1)).findLastSubmittedPsrFbInPreviousYears(any(), any())
    }

    "onPageLoads returns OK and expected content - when in compile responses returned" in runningApplication {
      implicit app =>
        when(mockPsrOverviewService.getOverview(any(), any(), any(), any())(using any(), any(), any())).thenReturn(
          Future.successful(Some(compiledOverviewResponse))
        )
        when(mockPsrVersionsService.getVersionsForYears(any(), any(), any())(using any(), any(), any())).thenReturn(
          Future.successful(compiledVersionsForYearsInProgressResponse)
        )
        when(mockPrePopulationService.findLastSubmittedPsrFbInPreviousYears(any(), any())).thenReturn(
          Some("1")
        )
        val request = FakeRequest(GET, onPageLoad)

        val result = route(app, request).value

        status(result) mustEqual OK
        val content = contentAsString(result)
        content must include("/select-continue")
        verify(mockPsrOverviewService, times(1)).getOverview(any(), any(), any(), any())(using any(), any(), any())
        verify(mockPsrVersionsService, times(1)).getVersionsForYears(any(), any(), any())(using any(), any(), any())
        verify(mockPrePopulationService, times(1)).findLastSubmittedPsrFbInPreviousYears(any(), any())
    }

    "onSelectStart redirects to what you will need page" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onSelectStart(None))

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.WhatYouWillNeedController
        .onPageLoad(srn)
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

    "onSelectStart redirects to what you will need page when report type is Sipp" in runningApplication {
      implicit app =>
        val request = FakeRequest(GET, onSelectStartSipp(None))

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual sippStartUrl
    }

    List(commonVersion, "002").foreach { version =>
      s"onSelectContinue redirects to task list page with version $version" in runningApplication { implicit app =>
        val request = FakeRequest(GET, onSelectContinue(version))

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.nonsipp.routes.TaskListController.onPageLoad(srn).url
      }
    }

    List(
      (commonVersion, sippStartUrl),
      ("002", sippViewOrChangeUrl)
    ).foreach { (version, expectedRedirect) =>
      s"onSelectContinue for SIPP type redirects to expected page when version is $version" in runningApplication {
        implicit app =>
          val request = FakeRequest(GET, onSelectContinueSipp(version))

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual expectedRedirect
      }
    }

    "onSelectContinue redirects to unauthorised when version is not an integer" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onSelectContinue("invalidInt"))

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad().url
    }

    "onSelectViewAndChange redirects to the BasicDetailsCYA page when members over threshold" in {
      when(mockPsrVersionsService.getVersions(any(), any(), any())(using any(), any(), any())).thenReturn(
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
      val preUA = currentUA.unsafeSet(DoesMemberHaveNinoPage(srn, 1), true)

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
      when(mockPsrVersionsService.getVersions(any(), any(), any())(using any(), any(), any())).thenReturn(
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
        when(mockPsrVersionsService.getVersions(any(), any(), any())(using any(), any(), any())).thenReturn(
          Future.successful(versionsResponse)
        )
        when(
          mockPsrRetrievalService
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using any(), any(), any())
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
        when(mockPsrVersionsService.getVersions(any(), any(), any())(using any(), any(), any())).thenReturn(
          Future.successful(versionsResponse)
        )
        when(
          mockPsrRetrievalService
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(using any(), any(), any())
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

    "onSelectViewAndChange redirects to the Sipp view or change page when type is Sipp" in runningApplication {
      implicit app =>
        val request = FakeRequest(GET, onSelectViewAndChangeSipp)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual sippViewOrChangeUrl
    }
  }
}

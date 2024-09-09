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

package controllers.nonsipp

import services.{PsrRetrievalService, PsrVersionsService}
import play.api.mvc.Call
import controllers.ControllerBaseSpec
import views.html.SubmissionView
import models.backend.responses.PsrVersionsResponse
import controllers.nonsipp.ViewOnlyReturnSubmittedController.viewModel
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.WhichTaxYearPage
import play.api.inject

import scala.concurrent.Future

class ViewOnlyReturnSubmittedControllerSpec extends ControllerBaseSpec with CommonTestValues {

  private val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]
  private val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]

  override val additionalBindings: List[GuiceableModule] =
    List(
      inject.bind[PsrVersionsService].toInstance(mockPsrVersionsService),
      inject.bind[PsrRetrievalService].toInstance(mockPsrRetrievalService)
    )

  override def beforeAll(): Unit = {
    when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(versionsResponse))
      .thenReturn(Future.successful(versionsResponse))
      .thenReturn(Future.successful(Seq.empty[PsrVersionsResponse]))
      .thenReturn(Future.successful(Seq.empty[PsrVersionsResponse]))
    when(
      mockPsrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
    ).thenReturn(Future.successful(userAnswers))
      .thenReturn(Future.successful(emptyUserAnswers))
      .thenReturn(Future.successful(userAnswers))
      .thenReturn(Future.successful(emptyUserAnswers))
  }

  lazy val onPageLoad: Call = routes.ViewOnlyReturnSubmittedController.onPageLoad(srn, yearString, versionNumber)

  private val userAnswers: UserAnswers = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)
  private val versionNumber: Int = 1
  private val pensionSchemeEnquiriesUrl =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/pension-scheme-enquiries"
  private val mpsDashboardUrl = "http://localhost:8204/manage-pension-schemes/pension-scheme-summary/" + srn.value

  "ViewOnlyReturnSubmittedController" - {

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[SubmissionView]
          .apply(
            viewModel(
              srn,
              schemeName,
              userAnswers,
              versionsResponse,
              versionNumber,
              pensionSchemeEnquiriesUrl,
              mpsDashboardUrl
            )
          )
      }.withName("onPageLoad renders ok")
    )
  }

  act.like(
    renderView(onPageLoad, emptyUserAnswers) { implicit app => implicit request =>
      injected[SubmissionView]
        .apply(
          viewModel(
            srn,
            schemeName,
            emptyUserAnswers,
            versionsResponse,
            versionNumber,
            pensionSchemeEnquiriesUrl,
            mpsDashboardUrl
          )
        )
    }.withName("onPageLoad renders ok with empty userAnswers")
  )

  act.like(
    renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[SubmissionView]
        .apply(
          viewModel(
            srn,
            schemeName,
            userAnswers,
            Seq.empty[PsrVersionsResponse],
            versionNumber,
            pensionSchemeEnquiriesUrl,
            mpsDashboardUrl
          )
        )
    }.withName("onPageLoad renders ok with empty versionsResponse")
  )

  act.like(
    renderView(onPageLoad, emptyUserAnswers) { implicit app => implicit request =>
      injected[SubmissionView]
        .apply(
          viewModel(
            srn,
            schemeName,
            emptyUserAnswers,
            Seq.empty[PsrVersionsResponse],
            versionNumber,
            pensionSchemeEnquiriesUrl,
            mpsDashboardUrl
          )
        )
    }.withName("onPageLoad renders ok with empty userAnswers and empty versionsResponse")
  )
}

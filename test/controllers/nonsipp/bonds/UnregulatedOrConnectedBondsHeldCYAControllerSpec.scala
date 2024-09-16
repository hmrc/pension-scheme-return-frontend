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

package controllers.nonsipp.bonds

import services.PsrSubmissionService
import config.Refined.OneTo5000
import play.api.inject.bind
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models._
import controllers.nonsipp.bonds.UnregulatedOrConnectedBondsHeldCYAController._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds._
import controllers.ControllerBaseSpec
import models.SchemeHoldBond.Acquisition

class UnregulatedOrConnectedBondsHeldCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private val index = refineMV[OneTo5000](1)
  private val page = 1

  private def onPageLoad(mode: Mode) =
    routes.UnregulatedOrConnectedBondsHeldCYAController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode) = routes.UnregulatedOrConnectedBondsHeldCYAController.onSubmit(srn, index, mode)

  private lazy val onPageLoadViewOnly = routes.UnregulatedOrConnectedBondsHeldCYAController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onSubmitViewOnly = routes.UnregulatedOrConnectedBondsHeldCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(NameOfBondsPage(srn, index), otherName)
    .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), Acquisition)
    .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)
    .unsafeSet(CostOfBondsPage(srn, index), money)
    .unsafeSet(BondsFromConnectedPartyPage(srn, index), true)
    .unsafeSet(AreBondsUnregulatedPage(srn, index), true)
    .unsafeSet(IncomeFromBondsPage(srn, index), money)

  "UnregulatedOrConnectedBondsHeldCYAController" - {
    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              index,
              schemeName,
              otherName,
              Acquisition,
              Some(localDate),
              money,
              Some(true),
              areBondsUnregulated = true,
              money,
              mode,
              viewOnlyUpdated = true
            )
          )
        }.withName(s"render correct $mode view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPsrSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in $mode mode")
      )
    }
  }

  "UnregulatedOrConnectedBondsHeldCYAController in view only mode" - {

    val currentUserAnswers = filledUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              index,
              schemeName,
              otherName,
              Acquisition,
              Some(localDate),
              money,
              Some(true),
              areBondsUnregulated = true,
              money,
              ViewOnlyMode,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.bonds.routes.BondsListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne, showBackLink = true)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to bond list page")
    )
  }
}

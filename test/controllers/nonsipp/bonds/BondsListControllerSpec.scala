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
import config.Refined.Max5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.bonds.BondsListController._
import views.html.ListView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models._
import viewmodels.models.SectionCompleted
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject

import scala.concurrent.Future

class BondsListControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val page = 1
  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private lazy val onPageLoad =
    routes.BondsListController.onPageLoad(srn, page, NormalMode)

  private lazy val onSubmit =
    routes.BondsListController.onSubmit(srn, page, NormalMode)

  private lazy val onSubmitViewOnly = routes.BondsListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.BondsListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.BondsListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(BondsCompleted(srn, index), SectionCompleted)
      .unsafeSet(NameOfBondsPage(srn, index), "Name")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
      .unsafeSet(CostOfBondsPage(srn, index), money)
      .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false)

  private val bondsData = List(
    BondsData(
      index,
      nameOfBonds = "Name",
      acquisitionType = SchemeHoldBond.Acquisition,
      costOfBonds = money
    )
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "BondsListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            page,
            NormalMode,
            bondsData,
            schemeName,
            viewOnlyViewModel = None
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, BondsListPage(srn), true, userAnswers) { implicit app => implicit request =>
        injected[ListView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(
              srn,
              page,
              NormalMode,
              bondsData,
              schemeName,
              viewOnlyViewModel = None
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

//    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "BondsListController in view only mode" - {
    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
      .unsafeSet(BondsCompleted(srn, index), SectionCompleted)
      .unsafeSet(NameOfBondsPage(srn, index), "Name")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
      .unsafeSet(CostOfBondsPage(srn, index), money)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
      .unsafeSet(BondsCompleted(srn, index), SectionCompleted)

    val viewOnlyViewModel = ViewOnlyViewModel(
      viewOnlyUpdated = false,
      year = yearString,
      currentVersion = submissionNumberOne,
      previousVersion = submissionNumberZero,
      compilationOrSubmissionDate = Some(submissionDateTwo)
    )

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              bondsData,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel)
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(BondsCompleted(srn, index), SectionCompleted)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(defaultUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              bondsData,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true))
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only tasklist")
    )

    act.like(
      redirectToPage(
        onPreviousViewOnly,
        controllers.nonsipp.bonds.routes.BondsListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero)
      ).withName(
        "Submit previous view only redirects to the controller with parameters for the previous submission"
      )
    )
  }
}

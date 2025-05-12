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
import controllers.nonsipp.bonds.BondsListController._
import views.html.ListView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds._
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject

class BondsListControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val indexTwo = refineMV[Max5000.Refined](2)
  private val indexThree = refineMV[Max5000.Refined](3)
  private val page = 1
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

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
      .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
      .unsafeSet(BondsCompleted(srn, index), SectionCompleted)
      .unsafeSet(NameOfBondsPage(srn, index), "Name")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)
      .unsafeSet(CostOfBondsPage(srn, index), money)
      .unsafeSet(BondsFromConnectedPartyPage(srn, index), true)
      .unsafeSet(AreBondsUnregulatedPage(srn, index), true)
      .unsafeSet(IncomeFromBondsPage(srn, index), money)
      .unsafeSet(BondsProgress(srn, index), SectionJourneyStatus.Completed)

  private val userAnswersHalfChecked =
    userAnswers
      .unsafeSet(BondsCompleted(srn, indexTwo), SectionCompleted)
      .unsafeSet(NameOfBondsPage(srn, indexTwo), "NameTwo")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, indexTwo), SchemeHoldBond.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, indexTwo), localDate)
      .unsafeSet(CostOfBondsPage(srn, indexTwo), money)
      .unsafeSet(BondsFromConnectedPartyPage(srn, indexTwo), true)
      .unsafeSet(AreBondsUnregulatedPage(srn, indexTwo), true)
      .unsafeSet(BondPrePopulated(srn, indexTwo), false)
      .unsafeSet(BondsProgress(srn, indexTwo), SectionJourneyStatus.Completed)
      .unsafeSet(BondsCompleted(srn, indexThree), SectionCompleted)
      .unsafeSet(NameOfBondsPage(srn, indexThree), "NameThree")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, indexThree), SchemeHoldBond.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, indexThree), localDate)
      .unsafeSet(CostOfBondsPage(srn, indexThree), money)
      .unsafeSet(BondsFromConnectedPartyPage(srn, indexThree), true)
      .unsafeSet(AreBondsUnregulatedPage(srn, indexThree), true)
      .unsafeSet(BondPrePopulated(srn, indexThree), true)
      .unsafeSet(BondsProgress(srn, indexThree), SectionJourneyStatus.Completed)

  private val bondsData = List(
    BondsData(
      index,
      nameOfBonds = "Name",
      acquisitionType = SchemeHoldBond.Acquisition,
      costOfBonds = money,
      canRemove = true
    )
  )

  private val bondsDataChecked: List[BondsData] = List(
    BondsData(
      indexThree,
      nameOfBonds = "NameThree",
      acquisitionType = SchemeHoldBond.Acquisition,
      costOfBonds = money,
      canRemove = false
    )
  )

  private val bondsDataToCheck = List(
    BondsData(
      indexTwo,
      nameOfBonds = "NameTwo",
      acquisitionType = SchemeHoldBond.Acquisition,
      costOfBonds = money,
      canRemove = false
    )
  )

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "BondsListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, page, NormalMode, bondsData, Nil, schemeName, showBackLink = true, isPrePop = false)
        )
    })

    act.like(renderViewWithPrePopSession(onPageLoad, userAnswersHalfChecked) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            page,
            NormalMode,
            bondsData ++ bondsDataChecked,
            bondsDataToCheck,
            schemeName,
            showBackLink = true,
            isPrePop = true
          )
        )
    }.updateName(_ + " - PrePop"))

    act.like(
      renderPrePopView(onPageLoad, BondsListPage(srn), true, userAnswers) { implicit app => implicit request =>
        injected[ListView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, page, NormalMode, bondsData, Nil, schemeName, showBackLink = true, isPrePop = false)
          )
      }
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "BondsListController in view only mode" - {
    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    val viewOnlyViewModel = ViewOnlyViewModel(
      viewOnlyUpdated = false,
      year = yearString,
      currentVersion = submissionNumberTwo,
      previousVersion = submissionNumberOne,
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
              Nil,
              schemeName,
              Some(viewOnlyViewModel),
              showBackLink = true,
              isPrePop = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(NameOfBondsPage(srn, index), "Name")

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
              Nil,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true,
              isPrePop = false
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
      renderView(
        onPreviousViewOnly,
        userAnswers = currentUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView].apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            1,
            mode = ViewOnlyMode,
            bondsData,
            Nil,
            schemeName,
            Some(
              viewOnlyViewModel.copy(
                currentVersion = (submissionNumberTwo - 1).max(0),
                previousVersion = (submissionNumberOne - 1).max(0)
              )
            ),
            showBackLink = false,
            isPrePop = false
          )
        )
      }.withName("Submit previous view only renders the controller with parameters for the previous submission")
    )

  }
}

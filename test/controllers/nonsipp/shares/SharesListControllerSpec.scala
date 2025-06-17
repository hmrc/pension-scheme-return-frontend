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

package controllers.nonsipp.shares

import services.PsrSubmissionService
import pages.nonsipp.shares._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ListView
import utils.IntUtils.given
import controllers.nonsipp.shares.SharesListController._
import forms.YesNoPageFormProvider
import models._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject

class SharesListControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val indexOne = 1
  private val indexTwo = 2
  private val indexThree = 3

  private val page = 1
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private lazy val onPageLoad =
    controllers.nonsipp.shares.routes.SharesListController.onPageLoad(srn, page, NormalMode)

  private lazy val onSubmit =
    controllers.nonsipp.shares.routes.SharesListController.onSubmit(srn, page, NormalMode)

  private lazy val onSubmitViewOnly =
    controllers.nonsipp.shares.routes.SharesListController.onSubmitViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPageLoadViewOnly =
    controllers.nonsipp.shares.routes.SharesListController.onPageLoadViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPreviousViewOnly =
    controllers.nonsipp.shares.routes.SharesListController.onPreviousViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
      .unsafeSet(SharesCompleted(srn, indexOne), SectionCompleted)
      .unsafeSet(SharesProgress(srn, indexOne), SectionJourneyStatus.Completed)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, indexOne), SchemeHoldShare.Transfer)
      .unsafeSet(TypeOfSharesHeldPage(srn, indexOne), TypeOfShares.ConnectedParty)
      .unsafeSet(CompanyNameRelatedSharesPage(srn, indexOne), companyName)
      .unsafeSet(SharesCompanyCrnPage(srn, indexOne), ConditionalYesNo.yes[String, Crn](crn))
      .unsafeSet(ClassOfSharesPage(srn, indexOne), classOfShares)
      .unsafeSet(HowManySharesPage(srn, indexOne), totalShares)
      .unsafeSet(CostOfSharesPage(srn, indexOne), money)
      .unsafeSet(SharesIndependentValuationPage(srn, indexOne), true)
      .unsafeSet(SharesTotalIncomePage(srn, indexOne), money)

  private val userAnswersToCheck = userAnswers
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, indexTwo), SchemeHoldShare.Transfer)
    .unsafeSet(TypeOfSharesHeldPage(srn, indexTwo), TypeOfShares.ConnectedParty)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, indexTwo), companyName)
    .unsafeSet(SharesCompanyCrnPage(srn, indexTwo), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(ClassOfSharesPage(srn, indexTwo), classOfShares)
    .unsafeSet(HowManySharesPage(srn, indexTwo), totalShares)
    .unsafeSet(CostOfSharesPage(srn, indexTwo), money)
    .unsafeSet(SharesIndependentValuationPage(srn, indexTwo), true)
    .unsafeSet(SharePrePopulated(srn, indexTwo), false)
    .unsafeSet(SharesProgress(srn, indexTwo), SectionJourneyStatus.Completed)

  private val incompleteUserAnswers = userAnswers
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, indexTwo), SchemeHoldShare.Transfer)
    .unsafeSet(TypeOfSharesHeldPage(srn, indexTwo), TypeOfShares.ConnectedParty)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, indexTwo), companyName)
    .unsafeSet(
      SharesProgress(srn, indexTwo),
      SectionJourneyStatus.InProgress(SharesCompanyCrnPage(srn, indexTwo))
    )

  private val userAnswersChecked = userAnswersToCheck
    .unsafeSet(SharePrePopulated(srn, indexTwo), true)

  private val someIncompleteUserAnswersToCheck = userAnswersToCheck
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, indexThree), SchemeHoldShare.Transfer)
    .unsafeSet(TypeOfSharesHeldPage(srn, indexThree), TypeOfShares.ConnectedParty)
    .unsafeSet(
      SharesProgress(srn, indexThree),
      SectionJourneyStatus.InProgress(CompanyNameRelatedSharesPage(srn, indexThree))
    )

  private val noUserAnswers =
    defaultUserAnswers
      .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  private val sharesData = List(
    SharesData(
      indexOne,
      typeOfShares = TypeOfShares.ConnectedParty,
      companyName = companyName,
      acquisitionType = SchemeHoldShare.Transfer,
      acquisitionDate = None,
      canRemove = true
    )
  )
  private val changedSharesData = List(
    SharesData(
      indexOne,
      typeOfShares = TypeOfShares.ConnectedParty,
      companyName = "changed",
      acquisitionType = SchemeHoldShare.Transfer,
      acquisitionDate = None,
      canRemove = true
    )
  )
  private val shareToCheckData = List(
    SharesData(
      indexTwo,
      typeOfShares = TypeOfShares.ConnectedParty,
      companyName = companyName,
      acquisitionType = SchemeHoldShare.Transfer,
      acquisitionDate = None,
      canRemove = false
    )
  )

  private val shareToChecked = List(
    SharesData(
      indexTwo,
      typeOfShares = TypeOfShares.ConnectedParty,
      companyName = companyName,
      acquisitionType = SchemeHoldShare.Transfer,
      acquisitionDate = None,
      canRemove = false
    )
  )

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "SharesListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            page,
            NormalMode,
            sharesData,
            sharesToCheck = Nil,
            schemeName,
            showBackLink = true,
            isPrePop = false
          )
        )
    })

    act.like(renderView(onPageLoad, incompleteUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          1,
          NormalMode,
          sharesData,
          Nil,
          schemeName,
          showBackLink = true,
          isPrePop = false
        )
      )
    }.withName("Completed Journey - 1 added record and 1 incomplete record"))

    act.like(renderViewWithPrePopSession(onPageLoad, userAnswersToCheck) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            page,
            NormalMode,
            sharesData,
            shareToCheckData,
            schemeName,
            showBackLink = true,
            isPrePop = true
          )
        )
    }.updateName(_ + " - To Check"))

    act.like(renderViewWithPrePopSession(onPageLoad, someIncompleteUserAnswersToCheck) {
      implicit app => implicit request =>
        injected[ListView].apply(
          form(new YesNoPageFormProvider()),
          viewModel(
            srn,
            1,
            NormalMode,
            sharesData,
            shareToCheckData,
            schemeName,
            showBackLink = true,
            isPrePop = true
          )
        )
    }.withName("PrePop Journey - 1 added record, 1 added incomplete record, 1 PrePop record to Check"))

    act.like(renderViewWithPrePopSession(onPageLoad, userAnswersChecked) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            page,
            NormalMode,
            sharesData ++ shareToChecked,
            Nil,
            schemeName,
            showBackLink = true,
            isPrePop = true
          )
        )
    }.updateName(_ + " - Checked"))

    act.like(
      renderPrePopView(onPageLoad, SharesListPage(srn), true, userAnswers) { implicit app => implicit request =>
        injected[ListView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(
              srn,
              page,
              NormalMode,
              sharesData,
              sharesToCheck = Nil,
              schemeName,
              showBackLink = true,
              isPrePop = false
            )
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

  "SharesListController in view only mode" - {

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
          injected[ListView]
            .apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
                srn,
                page,
                mode = ViewOnlyMode,
                sharesData,
                sharesToCheck = Nil,
                schemeName,
                Some(viewOnlyViewModel),
                showBackLink = true,
                isPrePop = false
              )
            )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(CompanyNameRelatedSharesPage(srn, indexOne), "changed")

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView]
            .apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
                srn,
                page,
                mode = ViewOnlyMode,
                changedSharesData,
                sharesToCheck = Nil,
                schemeName,
                viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
                showBackLink = true,
                isPrePop = false
              )
            )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView]
            .apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
                srn,
                page,
                mode = ViewOnlyMode,
                shares = Nil,
                sharesToCheck = Nil,
                schemeName,
                viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
                showBackLink = true,
                isPrePop = false
              )
            )
      }.withName("OnPageLoadViewOnly renders ok with no shares")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
        verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
      ).withName("Submit redirects to view only taskList")
    )

    act.like(
      renderView(
        onPreviousViewOnly,
        userAnswers = currentUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView]
          .apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              sharesData,
              sharesToCheck = Nil,
              schemeName,
              viewOnlyViewModel = Some(
                viewOnlyViewModel.copy(
                  currentVersion = (submissionNumberTwo - 1).max(0),
                  previousVersion = (submissionNumberOne - 1).max(0)
                )
              ),
              showBackLink = false,
              isPrePop = false
            )
          )
      }.withName("OnPreviousViewOnly renders the view correctly")
    )
  }
}

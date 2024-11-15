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
import views.html.ListView
import controllers.nonsipp.shares.SharesListController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models._
import viewmodels.models.SectionCompleted
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject

class SharesListControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
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
      .unsafeSet(SharesCompleted(srn, index), SectionCompleted)
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted)
      .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)

  private val noUserAnswers =
    defaultUserAnswers
      .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  private val shareData = SharesData(
    index,
    typeOfShares = TypeOfShares.Unquoted,
    companyName = companyName,
    acquisitionType = SchemeHoldShare.Acquisition,
    acquisitionDate = Some(localDate)
  )
  private val sharesData = List(shareData)

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "SharesListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, page, NormalMode, sharesData, schemeName, showBackLink = true)
        )
    })

    act.like(
      renderPrePopView(onPageLoad, SharesListPage(srn), true, userAnswers) { implicit app => implicit request =>
        injected[ListView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, page, NormalMode, sharesData, schemeName, showBackLink = true)
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
                schemeName,
                Some(viewOnlyViewModel),
                showBackLink = true
              )
            )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(CompanyNameRelatedSharesPage(srn, index), "changed")

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
                List(shareData.copy(companyName = "changed")),
                schemeName,
                viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
                showBackLink = true
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
                List(),
                schemeName,
                viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
                showBackLink = true
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
      )
        .withName("Submit redirects to view only taskList")
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
              schemeName,
              viewOnlyViewModel = Some(
                viewOnlyViewModel.copy(
                  currentVersion = (submissionNumberTwo - 1).max(0),
                  previousVersion = (submissionNumberOne - 1).max(0)
                )
              ),
              showBackLink = false
            )
          )
      }.withName("OnPreviousViewOnly renders the view correctly")
    )
  }
}

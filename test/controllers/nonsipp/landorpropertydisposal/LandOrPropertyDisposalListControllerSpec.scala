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

package controllers.nonsipp.landorpropertydisposal

import views.html.ListView
import pages.nonsipp.landorpropertydisposal._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import config.RefinedTypes.{Max50, Max5000}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import utils.IntUtils.toRefined5000
import utils.IntUtils.given
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalListController._
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandOrPropertyCompleted, LandOrPropertyProgress}

class LandOrPropertyDisposalListControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.LandOrPropertyDisposalListController.onPageLoad(srn, page = 1)
  private lazy val onSubmit = routes.LandOrPropertyDisposalListController.onSubmit(srn, page = 1)
  private lazy val onPageLoadViewOnly =
    routes.LandOrPropertyDisposalListController.onPageLoadViewOnly(
      srn,
      page = 1,
      yearString,
      submissionNumberOne,
      submissionNumberZero
    )
  private lazy val onSubmitViewOnly =
    routes.LandOrPropertyDisposalListController.onSubmitViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPreviousViewOnly =
    routes.LandOrPropertyDisposalListController.onPreviousViewOnly(
      srn,
      page = 1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

  private val address1 = addressGen.sample.value

  private val one: Max5000 = 1

  private val addressesWithIndexes: List[((Max5000, List[Max50]), Address)] = List[((Max5000, List[Max50]), Address)](
    one -> List[Max50](1, 2) -> address1
  )

  private val addressesWithIndexesIncomplete: List[((Max5000, List[Max50]), Address)] =
    List[((Max5000, List[Max50]), Address)](
      one -> List[Max50](1) -> address1
    )

  private val userAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyCompleted(srn, 1), SectionCompleted)
    .unsafeSet(LandOrPropertyProgress(srn, 1), SectionJourneyStatus.Completed)
    .unsafeSet(LandPropertyDisposalCompletedPage(srn, 1, 2), SectionCompleted)
    .unsafeSet(LandPropertyDisposalCompletedPage(srn, 1, 1), SectionCompleted)
    .unsafeSet(LandOrPropertyDisposalProgress(srn, 1, 2), SectionJourneyStatus.Completed)
    .unsafeSet(LandOrPropertyDisposalProgress(srn, 1, 1), SectionJourneyStatus.Completed)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, 1), address1)
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, 1, 2), HowDisposed.Transferred)
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, 1, 1), HowDisposed.Transferred)
    .unsafeSet(LandOrPropertyDisposalPage(srn), true)

  "LandOrPropertyDisposalListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            NormalMode,
            page = 1,
            addressesWithIndexes,
            numberOfDisposals = 2,
            maxPossibleNumberOfDisposals = 50,
            userAnswers,
            schemeName,
            viewOnlyViewModel = None,
            showBackLink = true,
            maximumDisposalsReached = false,
            allPropertiesFullyDisposed = false
          )
        )
    })

    act.like(
      renderView(onPageLoad, userAnswers.unsafeRemove(LandOrPropertyDisposalProgress(srn, 1, 2))) {
        implicit app => implicit request =>
          injected[ListView]
            .apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
                srn,
                NormalMode,
                page = 1,
                addressesWithIndexesIncomplete,
                numberOfDisposals = 1,
                maxPossibleNumberOfDisposals = 50,
                userAnswers,
                schemeName,
                viewOnlyViewModel = None,
                showBackLink = true,
                maximumDisposalsReached = false,
                allPropertiesFullyDisposed = false
              )
            )
      }.updateName(_ + " - hide incomplete journeys")
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "view only mode" - {

      val currentUserAnswers = userAnswers
        .unsafeSet(FbVersionPage(srn), "002")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

      val previousUserAnswers = userAnswers
        .unsafeSet(FbVersionPage(srn), "001")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = false,
        year = yearString,
        currentVersion = submissionNumberOne,
        previousVersion = submissionNumberZero,
        compilationOrSubmissionDate = Some(submissionDateTwo)
      )

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = currentUserAnswers,
          optPreviousAnswers = Some(previousUserAnswers)
        ) { implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              ViewOnlyMode,
              page = 1,
              addressesWithIndexes,
              numberOfDisposals = 2,
              maxPossibleNumberOfDisposals = 50,
              userAnswers,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel),
              showBackLink = true,
              maximumDisposalsReached = false,
              allPropertiesFullyDisposed = false
            )
          )
        }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated false")
      )

      val updatedUserAnswers = currentUserAnswers

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = updatedUserAnswers,
          optPreviousAnswers = Some(
            previousUserAnswers.unsafeSet(HowWasPropertyDisposedOfPage(srn, 1, 1), HowDisposed.Sold)
          )
        ) { implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              ViewOnlyMode,
              page = 1,
              addressesWithIndexes,
              numberOfDisposals = 2,
              maxPossibleNumberOfDisposals = 50,
              userAnswers,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true,
              maximumDisposalsReached = false,
              allPropertiesFullyDisposed = false
            )
          )
        }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated true")
      )

      act.like(
        redirectToPage(
          onSubmitViewOnly,
          controllers.nonsipp.routes.ViewOnlyTaskListController
            .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
        ).withName("Submit redirects to view only tasklist")
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
              ViewOnlyMode,
              page = 1,
              addressesWithIndexes,
              numberOfDisposals = 2,
              maxPossibleNumberOfDisposals = 50,
              userAnswers,
              schemeName,
              viewOnlyViewModel = Some(
                viewOnlyViewModel.copy(
                  viewOnlyUpdated = false,
                  currentVersion = (submissionNumberTwo - 1).max(0),
                  previousVersion = (submissionNumberOne - 1).max(0)
                )
              ),
              showBackLink = false,
              maximumDisposalsReached = false,
              allPropertiesFullyDisposed = false
            )
          )
        }.withName("OnPreviousViewOnly renders the view correctly")
      )

    }
  }
}

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

package controllers.nonsipp.moneyborrowed

import services.PsrSubmissionService
import play.api.inject.bind
import views.html.CheckYourAnswersView
import utils.IntUtils.toInt
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models._
import pages.nonsipp.moneyborrowed._
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import controllers.nonsipp.moneyborrowed.MoneyBorrowedCYAController._
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec

class MoneyBorrowedCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private val index = refineMV[OneTo5000](1)

  private def onPageLoad(mode: Mode) =
    routes.MoneyBorrowedCYAController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode) =
    routes.MoneyBorrowedCYAController.onSubmit(srn, index, mode)

  private lazy val onSubmitViewOnly = routes.MoneyBorrowedCYAController.onSubmitViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnly = routes.MoneyBorrowedCYAController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(LenderNamePage(srn, index), lenderName)
    .unsafeSet(IsLenderConnectedPartyPage(srn, index), true)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))
    .unsafeSet(WhenBorrowedPage(srn, index), localDate)
    .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), money)
    .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index), schemeName)

  private val incompleteUserAnswers = defaultUserAnswers
    .unsafeSet(LenderNamePage(srn, index), lenderName)
    .unsafeSet(IsLenderConnectedPartyPage(srn, index), true)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))
    .unsafeSet(WhenBorrowedPage(srn, index), localDate)
    .unsafeSet(
      MoneyBorrowedProgress(srn, index),
      SectionJourneyStatus.InProgress(
        routes.ValueOfSchemeAssetsWhenMoneyBorrowedController
          .onPageLoad(srn, index, NormalMode)
          .url
      )
    )

  "MoneyBorrowedCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              index,
              schemeName,
              lenderName,
              lenderConnectedParty = true,
              borrowedAmountAndRate = (money, percentage),
              whenBorrowed = localDate,
              schemeAssets = money,
              schemeBorrowed = schemeName,
              mode,
              viewOnlyUpdated = true
            )
          )
        }.withName(s"render correct ${mode.toString} view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPsrSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in ${mode.toString} mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${mode.toString} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${mode.toString} mode")
      )

      redirectToPage(
        call = onPageLoad(mode),
        page = routes.ValueOfSchemeAssetsWhenMoneyBorrowedController.onPageLoad(srn, index, mode),
        userAnswers = incompleteUserAnswers,
        previousUserAnswers = emptyUserAnswers
      ).withName(s"redirect to list page when in $mode mode and incomplete data")
    }
  }

  "MoneyBorrowedCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(LenderNamePage(srn, index), lenderName)
      .unsafeSet(IsLenderConnectedPartyPage(srn, index), true)
      .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))
      .unsafeSet(WhenBorrowedPage(srn, index), localDate)
      .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), money)
      .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index), schemeName)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), money)
    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              index,
              schemeName,
              lenderName,
              lenderConnectedParty = true,
              borrowedAmountAndRate = (money, percentage),
              whenBorrowed = localDate,
              schemeAssets = money,
              schemeBorrowed = schemeName,
              ViewOnlyMode,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only BorrowInstancesListController page")
    )
  }

}

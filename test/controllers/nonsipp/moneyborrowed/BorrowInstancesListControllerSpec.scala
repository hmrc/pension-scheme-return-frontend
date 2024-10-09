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
import views.html.ListView
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.moneyborrowed._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import controllers.nonsipp.moneyborrowed.BorrowInstancesListController._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject

import scala.concurrent.Future

class BorrowInstancesListControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val page = 1
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private lazy val onPageLoad =
    controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, page, NormalMode)

  private lazy val onSubmit =
    controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onSubmit(srn, page, NormalMode)

  private lazy val moneyBorrowedPageLoad =
    controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, NormalMode)
  private lazy val onSubmitViewOnly =
    controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onSubmitViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPageLoadViewOnly =
    controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoadViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPreviousViewOnly =
    controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPreviousViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(MoneyBorrowedPage(srn), false)
      .unsafeSet(LenderNamePages(srn), Map("0" -> lenderName))
      .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  private val noBorrowingsUserAnswers = defaultUserAnswers
    .unsafeSet(MoneyBorrowedPage(srn), false)
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  "BorrowInstancesListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            NormalMode,
            page = 1,
            borrowingInstances = List((index, lenderName, money)),
            schemeName,
            showBackLink = true
          )
        )
    })

    act.like(
      redirectToPage(
        onPageLoad,
        moneyBorrowedPageLoad,
        noBorrowingsUserAnswers
      ).withName("No borrowInstances added redirects to yes-no page")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "BorrowInstancesListController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
      .unsafeSet(MoneyBorrowedPage(srn), false)
      .unsafeSet(LenderNamePages(srn), Map("0" -> lenderName))
      .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
      .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))

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
                mode = ViewOnlyMode,
                page = 1,
                borrowingInstances = List((index, lenderName, money)),
                schemeName,
                Some(viewOnlyViewModel),
                showBackLink = true
              )
            )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, Percentage(10)))

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView]
            .apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
                srn,
                mode = ViewOnlyMode,
                page = 1,
                borrowingInstances = List((index, lenderName, money)),
                schemeName,
                viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
                showBackLink = true
              )
            )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = noBorrowingsUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView].apply(
          form(new YesNoPageFormProvider()),
          viewModel(
            srn,
            mode = ViewOnlyMode,
            page = 1,
            borrowingInstances = List(),
            schemeName,
            viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
            showBackLink = true
          )
        )
      }.withName("OnPageLoadViewOnly renders ok with no borrowings")
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
        injected[ListView]
          .apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              mode = ViewOnlyMode,
              page = 1,
              borrowingInstances = List((index, lenderName, money)),
              schemeName,
              viewOnlyViewModel = Some(
                viewOnlyViewModel.copy(
                  currentVersion = submissionNumberOne,
                  previousVersion = submissionNumberZero
                )
              ),
              showBackLink = false
            )
          )
      }.withName("OnPreviousViewOnly renders view with parameters for the previous submission")
    )

  }

}

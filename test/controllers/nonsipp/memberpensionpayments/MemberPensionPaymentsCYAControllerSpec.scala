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

package controllers.nonsipp.memberpensionpayments

import services.PsrSubmissionService
import controllers.nonsipp.memberpensionpayments.MemberPensionPaymentsCYAController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.CheckYourAnswersView
import utils.IntUtils.given
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import pages.nonsipp.FbVersionPage
import models._
import viewmodels.models.SectionCompleted
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import org.mockito.Mockito._

class MemberPensionPaymentsCYAControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val page = 1

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override def beforeEach(): Unit =
    reset(mockPsrSubmissionService)

  private def onPageLoad(mode: Mode) =
    routes.MemberPensionPaymentsCYAController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode) =
    routes.MemberPensionPaymentsCYAController.onSubmit(srn, index, mode)

  private lazy val onPageLoadViewOnly = routes.MemberPensionPaymentsCYAController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onSubmitViewOnly = routes.MemberPensionPaymentsCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(TotalAmountPensionPaymentsPage(srn, index), money)
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)

  "MemberPensionPaymentsCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            MemberPensionPaymentsCYAController.viewModel(
              srn,
              memberDetails.fullName,
              index,
              money,
              mode,
              viewOnlyUpdated = true
            )
          )
        }.withName(s"render correct $mode view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
          .withName(s"redirect to next page when in $mode mode")
          .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(1)))
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

  "MemberPensionPaymentsCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)
      .unsafeSet(TotalAmountPensionPaymentsPage(srn, index), money)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              memberDetails.fullName,
              index,
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
        call = onSubmitViewOnly,
        page = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(never))
        .withName("Submit redirects to to Pension payments Member List page")
    )
  }
}

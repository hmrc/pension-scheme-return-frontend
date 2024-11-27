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

package controllers.nonsipp.receivetransfer

import play.api.test.FakeRequest
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import controllers.ControllerBaseSpec
import views.html.TwoColumnsTripleAction
import pages.nonsipp.receivetransfer.{DidSchemeReceiveTransferPage, ReceiveTransferProgress}
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import models._
import controllers.nonsipp.receivetransfer.TransferReceivedMemberListController._
import eu.timepit.refined.refineMV
import viewmodels.DisplayMessage.Message
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}

class TransferReceivedMemberListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.TransferReceivedMemberListController.onPageLoad(srn, page, NormalMode)
  private lazy val onSubmit = routes.TransferReceivedMemberListController.onSubmit(srn, page, NormalMode)
  private lazy val onSubmitViewOnly = routes.TransferReceivedMemberListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.TransferReceivedMemberListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.TransferReceivedMemberListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val page = 1

  val userAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
    .unsafeSet(
      ReceiveTransferProgress(srn, refineMV(1), refineMV(1)),
      SectionJourneyStatus.Completed
    )

  val testMemberList: List[MemberWithReceiveTransfer] = List(
    MemberWithReceiveTransfer(
      memberIndex = refineMV(1),
      transferFullName = memberDetails.fullName,
      receive = List(
        ReceiveTransfer(
          receiveIndex = refineMV(1),
          status = SectionJourneyStatus.Completed
        )
      )
    )
  )

  "TransferReceivedMemberListController" - {

    "viewModel should show 'No transfers' when there are no transfers" in {

      val memberList: List[MemberWithReceiveTransfer] = List.empty

      val result = TransferReceivedMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = true
      )

      result.optViewOnlyDetails.value.heading mustBe Message("transferIn.MemberList.viewOnly.noTransfers")
      result.page.rows.size mustBe 0
    }

    "viewModel should show '1 Transfer in' when there is 1 transfer in" in {

      val result = TransferReceivedMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        testMemberList,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false
      )
      result.optViewOnlyDetails.value.heading mustBe Message("transferIn.MemberList.viewOnly.singular")
    }

    "viewModel should show '2 Transfers in' when there are 2 transfers" in {
      val memberList: List[MemberWithReceiveTransfer] = List.fill(2)(
        MemberWithReceiveTransfer(
          memberIndex = refineMV(1),
          transferFullName = "Test Member",
          receive = List(
            ReceiveTransfer(
              receiveIndex = refineMV(1),
              status = SectionJourneyStatus.Completed
            )
          )
        )
      )

      val result = TransferReceivedMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false
      )
      result.optViewOnlyDetails.value.heading mustBe Message("transferIn.MemberList.viewOnly.plural", Message("2"))
    }

    "must return OK and render the correct view without back link" in {

      val currentUserAnswers = userAnswers
        .unsafeSet(FbVersionPage(srn), "002")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

      val previousUserAnswers = userAnswers
        .unsafeSet(FbVersionPage(srn), "001")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

      val application =
        applicationBuilder(userAnswers = Some(currentUserAnswers), previousUserAnswers = Some(previousUserAnswers))
          .build()

      running(application) {

        val request = FakeRequest(GET, onPreviousViewOnly.url)

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) must include("Submitted on")

        (contentAsString(result) must not).include("govuk-back-link")
      }
    }

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[TwoColumnsTripleAction].apply(
        viewModel(
          srn,
          page,
          NormalMode,
          testMemberList,
          viewOnlyUpdated = false,
          schemeName = schemeName,
          noPageEnabled = false
        )
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "TransferReceivedMemberListController in view only mode" - {

    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            TransferReceivedMemberListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              testMemberList,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              schemeName = schemeName,
              noPageEnabled = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            TransferReceivedMemberListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              testMemberList,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              schemeName = schemeName,
              noPageEnabled = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    val noUserAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
      .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
      .unsafeSet(DidSchemeReceiveTransferPage(srn), false)
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            TransferReceivedMemberListController.viewModel(
              srn,
              page = 1,
              mode = ViewOnlyMode,
              List.empty,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              schemeName = schemeName,
              noPageEnabled = true
            )
          )
      }.withName("OnPageLoadViewOnly renders ok NO records")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).withName("Submit redirects to view only taskList")
    )

  }
}

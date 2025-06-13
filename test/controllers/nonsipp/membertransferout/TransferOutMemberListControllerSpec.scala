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

package controllers.nonsipp.membertransferout

import play.api.test.FakeRequest
import controllers.{ControllerBaseSpec, ControllerBehaviours, MemberListBaseSpec}
import views.html.TwoColumnsTripleAction
import utils.IntUtils.given
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import models._
import pages.nonsipp.membertransferout._
import controllers.nonsipp.membertransferout.TransferOutMemberListController._
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}

class TransferOutMemberListControllerSpec extends ControllerBaseSpec with ControllerBehaviours with MemberListBaseSpec {

  private lazy val onPageLoad = routes.TransferOutMemberListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.TransferOutMemberListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onSubmitViewOnly = routes.TransferOutMemberListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.TransferOutMemberListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.TransferOutMemberListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private val index = 1
  private val page = 1

  val userAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)
    .unsafeSet(SchemeTransferOutPage(srn), true)
    .unsafeSet(MemberTransferOutProgress(srn, 1, 1), SectionJourneyStatus.Completed)

  val testMemberList: List[MemberWithTransferOut] = List(
    MemberWithTransferOut(
      memberIndex = 1,
      transferFullName = memberDetails.fullName,
      transfer = List(
        TransferOut(
          memberIndex = 1,
          status = SectionJourneyStatus.Completed
        )
      )
    )
  )

  "TransferOutMemberListController" - {

    "viewModel should show 'No transfers' when there are no transfers" in {

      val memberList: List[MemberWithTransferOut] = List.empty

      val result = TransferOutMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = true
      )

      result.optViewOnlyDetails.value.heading mustBe Message("transferOut.memberList.viewOnly.heading")
      result.page.rows.size mustBe 0
    }

    "viewModel should show 1 transfer when there is 1 transfer" in {

      val result = TransferOutMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        testMemberList,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message("transferOut.memberList.viewOnly.singular")
    }

    "viewModel should show 2 transfers when there are 2 transfers" in {

      val memberList: List[MemberWithTransferOut] = List.fill(2)(
        MemberWithTransferOut(
          memberIndex = 1,
          transferFullName = "Test Member",
          transfer = List(
            TransferOut(
              memberIndex = 1,
              status = SectionJourneyStatus.Completed
            )
          )
        )
      )

      val result = TransferOutMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message("transferOut.memberList.viewOnly.plural", Message("2"))
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

    "TransferOutMemberListControllerSpec in view only mode" - {
      val currentUserAnswers = userAnswers
        .unsafeSet(FbVersionPage(srn), "002")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

      val previousUserAnswers = userAnswers
        .unsafeSet(FbVersionPage(srn), "001")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

      "must return OK and render the correct view without back link" in {

        val application =
          applicationBuilder(userAnswers = Some(currentUserAnswers), previousUserAnswers = Some(previousUserAnswers))
            .build()

        running(application) {

          val request = FakeRequest(GET, onPreviousViewOnly.url)

          val result = route(application, request).value

          status(result) mustEqual OK

          checkContent(contentAsString(result))
        }
      }

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = currentUserAnswers,
          optPreviousAnswers = Some(previousUserAnswers)
        ) { implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            viewModel(
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
        .unsafeSet(WhenWasTransferMadePage(srn, index, 1), localDate)

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = updatedUserAnswers,
          optPreviousAnswers = Some(previousUserAnswers)
        ) { implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              testMemberList,
              viewOnlyUpdated = true,
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
        .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
        .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)
        .unsafeSet(SchemeTransferOutPage(srn), false)
        .unsafeSet(FbVersionPage(srn), "002")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
            injected[TwoColumnsTripleAction].apply(
              viewModel(
                srn,
                page,
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
        ).withName("Submit redirects to view only tasklist")
      )

    }
  }
}

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
import views.html.TwoColumnsTripleAction
import eu.timepit.refined.refineMV
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import models._
import pages.nonsipp.membertransferout._
import controllers.nonsipp.membertransferout.TransferOutMemberListController.{CompletedTransfersOut, _}
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import config.RefinedTypes.{Max300, Max5}
import controllers.ControllerBaseSpec
import viewmodels.DisplayMessage.Message
import viewmodels.models.SectionCompleted

import java.time.LocalDate

class TransferOutMemberListControllerSpec extends ControllerBaseSpec {

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
  private val index = refineMV[Max300.Refined](1)
  private val page = 1

  val userAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(SchemeTransferOutPage(srn), true)
    .unsafeSet(TransfersOutSectionCompleted(srn, refineMV(1), refineMV(1)), SectionCompleted)

  val testMemberList: List[(Max300, NameDOB, CompletedTransfersOut)] = List(
    (refineMV[Max300.Refined](1), memberDetails, 1)
  )

  "TransferOutMemberListController" - {

    "viewModel should show 'No transfers' when there are no transfers" in {
      val memberList: List[(Max300, NameDOB, CompletedTransfersOut)] = List.empty

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

      val memberDetails1 = NameDOB("testFirstName1", "testLastName1", LocalDate.of(1990, 12, 12))
      val memberDetails2 = NameDOB("testFirstName2", "testLastName2", LocalDate.of(1991, 6, 15))

      val memberList: List[(Max300, NameDOB, CompletedTransfersOut)] = List(
        (refineMV[Max300.Refined](1), memberDetails1, 1),
        (refineMV[Max300.Refined](2), memberDetails2, 1)
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

          contentAsString(result) must include("Submitted on")
          (contentAsString(result) must not).include("govuk-back-link")
        }
      }

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
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
        .unsafeSet(WhenWasTransferMadePage(srn, index, refineMV[Max5.Refined](1)), localDate)

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
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
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
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

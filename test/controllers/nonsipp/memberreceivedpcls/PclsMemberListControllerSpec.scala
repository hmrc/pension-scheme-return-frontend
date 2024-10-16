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

package controllers.nonsipp.memberreceivedpcls

import play.api.test.FakeRequest
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import views.html.TwoColumnsTripleAction
import eu.timepit.refined.refineMV
import controllers.nonsipp.memberreceivedpcls.PclsMemberListController._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import models._
import config.RefinedTypes.Max300
import controllers.ControllerBaseSpec
import viewmodels.DisplayMessage.Message
import viewmodels.models.SectionCompleted

import java.time.LocalDate

class PclsMemberListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.PclsMemberListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.PclsMemberListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onSubmitViewOnly = routes.PclsMemberListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.PclsMemberListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.PclsMemberListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private val index = refineMV[Max300.Refined](1)
  private val page = 1
  val lumpSumData: PensionCommencementLumpSum = pensionCommencementLumpSumGen.sample.value

  private val userAnswers: UserAnswers =
    defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
      .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
      .unsafeSet(PensionCommencementLumpSumPage(srn), true)

  "PclsMemberListController" - {

    "viewModel should show 'Pension commencement lump sum' when there are 0 PCLS" in {

      val userAnswersWithNoPcls = userAnswers
        .unsafeSet(PensionCommencementLumpSumPage(srn), false)

      val memberList: List[Option[NameDOB]] = List.empty

      val result = PclsMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithNoPcls,
        viewOnlyUpdated = false,
        noPageEnabled = true
      )

      result.optViewOnlyDetails.value.heading mustBe Message("pcls.MemberList.viewOnly.heading")
      result.page.rows.size mustBe 0
    }

    "viewModel should show 1 Pension commencement lump sum when there is 1 PCLS" in {

      val memberDetails1 = NameDOB("testFirstName1", "testLastName1", LocalDate.of(1990, 12, 12))

      val userAnswersWithOnePcls = userAnswers
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails1)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
        .unsafeSet(PensionCommencementLumpSumAmountPage(srn, refineMV(1)), pensionCommencementLumpSumGen.sample.value)
        .unsafeSet(PensionCommencementLumpSumPage(srn), true)

      val memberList: List[Option[NameDOB]] = List(Some(memberDetails1))

      val result = PclsMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithOnePcls,
        viewOnlyUpdated = false,
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message("pcls.MemberList.viewOnly.withValue", Message("1"))
      result.page.rows.size mustBe 1
    }

    "viewModel should show 2 Pension commencement lump sums when there are 2 PCLS" in {

      val memberDetails1 = NameDOB("testFirstName1", "testLastName1", LocalDate.of(1990, 12, 12))
      val memberDetails2 = NameDOB("testFirstName2", "testLastName2", LocalDate.of(1991, 6, 15))

      val userAnswersWithTwoPcls = userAnswers
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails1)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
        .unsafeSet(PensionCommencementLumpSumAmountPage(srn, refineMV(1)), pensionCommencementLumpSumGen.sample.value)
        .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails2)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(2)), SectionCompleted)
        .unsafeSet(PensionCommencementLumpSumAmountPage(srn, refineMV(2)), pensionCommencementLumpSumGen.sample.value)
        .unsafeSet(PensionCommencementLumpSumPage(srn), true)

      val memberList: List[Option[NameDOB]] = List(Some(memberDetails1), Some(memberDetails2))

      val result = PclsMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithTwoPcls,
        viewOnlyUpdated = false,
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message("pcls.MemberList.viewOnly.withValue", Message("2"))
    }

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

      injected[TwoColumnsTripleAction].apply(
        viewModel(
          srn,
          page,
          NormalMode,
          memberList,
          userAnswers,
          viewOnlyUpdated = false,
          noPageEnabled = false
        )
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "PclsMemberListController in view only mode" - {

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

    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList: List[Option[NameDOB]] = currentUserAnswers.membersOptionList(srn)

          injected[TwoColumnsTripleAction].apply(
            PclsMemberListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              memberList,
              currentUserAnswers,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              noPageEnabled = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index), lumpSumData)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList: List[Option[NameDOB]] = updatedUserAnswers.membersOptionList(srn)

          injected[TwoColumnsTripleAction].apply(
            PclsMemberListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              memberList,
              updatedUserAnswers,
              viewOnlyUpdated = true,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              noPageEnabled = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    val noUserAnswers = currentUserAnswers
      .unsafeSet(PensionCommencementLumpSumPage(srn), false)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            PclsMemberListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              List.empty,
              noUserAnswers,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
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

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

package controllers.nonsipp.membercontributions

import play.api.test.FakeRequest
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import pages.nonsipp.membercontributions.{MemberContributionsPage, TotalMemberContributionPage}
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import views.html.TwoColumnsTripleAction
import utils.IntUtils.given
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import models._
import config.RefinedTypes.Max300
import controllers.{ControllerBaseSpec, ControllerBehaviours, MemberListBaseSpec}
import viewmodels.DisplayMessage.Message
import viewmodels.models.SectionCompleted

import java.time.LocalDate

class MemberContributionListControllerSpec
    extends ControllerBaseSpec
    with ControllerBehaviours
    with MemberListBaseSpec {

  private lazy val onPageLoad = routes.MemberContributionListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.MemberContributionListController.onSubmit(srn, NormalMode)
  private lazy val onSubmitViewOnly = routes.MemberContributionListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.MemberContributionListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.MemberContributionListController.onPreviousViewOnly(
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
    .unsafeSet(MemberContributionsPage(srn), true)

  "MemberContributionListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val memberList: List[(Max300, NameDOB)] = userAnswers.completedMembersDetails(srn).value

      injected[TwoColumnsTripleAction].apply(
        MemberContributionListController.viewModel(
          srn,
          page = 1,
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

  "MemberContributionListController in view only mode" - {

    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

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

        checkContent(contentAsString(result))
      }
    }

    "ViewModel should display 'Member Contributions' when there are 0 member contributions" in {
      val userAnswersWithNoContributions = userAnswers
        .unsafeSet(MemberContributionsPage(srn), true)
        .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
        .unsafeSet(TotalMemberContributionPage(srn, 1), Money(0))

      val memberList = userAnswersWithNoContributions.completedMembersDetails(srn).value

      val result = MemberContributionListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithNoContributions,
        viewOnlyUpdated = false,
        optYear = Some(yearString),
        optCurrentVersion = Some(submissionNumberTwo),
        optPreviousVersion = Some(submissionNumberOne),
        compilationOrSubmissionDate = Some(submissionDateTwo),
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message("ReportContribution.MemberList.viewOnly.noContributions")
      result.page.rows.size mustBe 1
    }

    "ViewModel should display 'Member contribution' for 1 member contribution" in {
      val userAnswersWithOneContribution = userAnswers
        .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
        .unsafeSet(TotalMemberContributionPage(srn, 1), Money(1))

      val memberList = userAnswersWithOneContribution.completedMembersDetails(srn).value

      val result = MemberContributionListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithOneContribution,
        viewOnlyUpdated = false,
        optYear = Some(yearString),
        optCurrentVersion = Some(submissionNumberTwo),
        optPreviousVersion = Some(submissionNumberOne),
        compilationOrSubmissionDate = Some(submissionDateTwo),
        noPageEnabled = false
      )
      result.optViewOnlyDetails.value.heading mustBe Message("ReportContribution.MemberList.viewOnly.singular")

    }

    "ViewModel should display '2 member contributions' for 2 member contributions" in {
      val memberDetails1 = NameDOB("testFirstName1", "testLastName1", LocalDate.of(1990, 12, 12))
      val memberDetails2 = NameDOB("testFirstName2", "testLastName2", LocalDate.of(1991, 6, 15))

      val userAnswersWithTwoContributions = userAnswers
        .unsafeSet(MemberDetailsPage(srn, 1), memberDetails1)
        .unsafeSet(TotalMemberContributionPage(srn, 1), Money(10))
        .unsafeSet(MemberDetailsPage(srn, 2), memberDetails2)
        .unsafeSet(TotalMemberContributionPage(srn, 2), Money(20))

      val memberList: List[(Max300, NameDOB)] =
        List((1, memberDetails1), (2, memberDetails2))

      val result = MemberContributionListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithTwoContributions,
        viewOnlyUpdated = false,
        optYear = Some(yearString),
        optCurrentVersion = Some(submissionNumberTwo),
        optPreviousVersion = Some(submissionNumberOne),
        compilationOrSubmissionDate = Some(submissionDateTwo),
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message(
        "ReportContribution.MemberList.viewOnly.plural",
        Message("2")
      )

    }

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList = currentUserAnswers.completedMembersDetails(srn).value

          injected[TwoColumnsTripleAction].apply(
            MemberContributionListController.viewModel(
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
      .unsafeSet(TotalMemberContributionPage(srn, index), money)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList = updatedUserAnswers.completedMembersDetails(srn).value

          injected[TwoColumnsTripleAction].apply(
            MemberContributionListController.viewModel(
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

    val noUserAnswers = currentUserAnswers.unsafeSet(MemberContributionsPage(srn), false)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            MemberContributionListController.viewModel(
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
